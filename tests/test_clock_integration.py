import configparser
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
WORKSPACE = ROOT.parent


def unit(path):
    parser = configparser.ConfigParser(strict=False, interpolation=None)
    parser.optionxform = str
    section = None
    for line in path.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith(('#', ';')):
            continue
        if line.startswith('[') and line.endswith(']'):
            section = line[1:-1]
            if not parser.has_section(section):
                parser.add_section(section)
        elif '=' in line and section:
            key, value = line.split('=', 1)
            previous = parser.get(section, key, fallback='')
            parser.set(section, key, (previous + ' ' + value).strip())
    return parser


def words(parser, section, key):
    return parser.get(section, key, fallback="").split()


class ClockIntegrationTest(unittest.TestCase):
    def test_image_packages_and_chrony_configuration(self):
        recipe = (ROOT / "recipes-core/pm-service/pm-service.bb").read_text()
        self.assertIn('${GO_IMPORT}/cmd/clock-coordinator', recipe)
        self.assertIn('FILES:clock-coordinator', recipe)
        self.assertIn('SYSTEMD_PACKAGES = "${PN} clock-coordinator"', recipe)
        self.assertIn('clock-coordinator', (ROOT / 'recipes-fsl/images/librescoot-dbc-image.bb').read_text())
        self.assertIn('pm-service', (ROOT / 'recipes-fsl/images/librescoot-mdb-image.bb').read_text())
        mdb = (ROOT / 'recipes-support/chrony/files/chrony-mdb.conf').read_text()
        dbc = (ROOT / 'recipes-support/chrony/files/chrony-dbc.conf').read_text()
        for config in (mdb, dbc):
            self.assertNotIn('makestep', config)
            self.assertIn('maxdistance 8', config)
        for source in ('nrf', 'gps', 'user'):
            self.assertIn('/run/chrony/' + source + '.sock', mdb)
        self.assertIn('refid GPS1 precision 4 poll 0 filter 1 minsamples 4 maxsamples 8', mdb)
        self.assertIn('local stratum 10', mdb)
        self.assertIn('server 192.168.7.1', dbc)

    def test_startup_order_has_no_cycle_or_network_gate(self):
        files = {
            'fake-hwclock-load.service': ROOT / 'recipes-extended/fake-hwclock/files/fake-hwclock-load.service',
            'clock-coordinator.service': ROOT / 'recipes-core/pm-service/files/clock-coordinator.service',
            'librescoot-bluetooth.service': ROOT / 'recipes-core/bluetooth-service/files/librescoot-bluetooth.service',
            'librescoot-modem.service': ROOT / 'recipes-core/modem-service/files/librescoot-modem.service',
            'librescoot-vehicle.service': ROOT / 'recipes-core/vehicle-service/files/librescoot-vehicle.service',
            'librescoot-settings.service': ROOT / 'recipes-core/settings-service/files/librescoot-settings.service',
            'librescoot-pm.service': WORKSPACE / 'pm-service/librescoot-pm.service',
            'dbc-dispatcher.service': ROOT / 'recipes-graphics/dbc-dispatcher/files/dbc-dispatcher.service',
            'boot-animation.service': ROOT / 'recipes-graphics/boot-animation/files/boot-animation.service',
            'gpsd.service': ROOT / 'recipes-navigation/gpsd/gpsd/gpsd.service.conf',
        }
        units = {name: unit(path) for name, path in files.items()}
        graph = {}
        def add(before, after):
            graph.setdefault(before, set()).add(after)
        for name, parser in units.items():
            for other in words(parser, 'Unit', 'After'):
                add(other, name)
            for other in words(parser, 'Unit', 'Before'):
                add(name, other)
            if parser.get('Unit', 'DefaultDependencies', fallback='yes').lower() != 'no':
                add('sysinit.target', name)
                add(name, 'shutdown.target')
        visited, active = set(), set()
        def visit(node):
            self.assertNotIn(node, active, f'ordering cycle at {node}')
            if node in visited:
                return
            active.add(node)
            for next_node in graph.get(node, ()):
                visit(next_node)
            active.remove(node)
            visited.add(node)
        for node in list(graph):
            visit(node)
        self.assertIn('chronyd.service', words(units['fake-hwclock-load.service'], 'Unit', 'Before'))
        self.assertIn('fake-hwclock-load.service', words(units['gpsd.service'], 'Unit', 'After'))
        self.assertIn('clock-coordinator.service', words(units['librescoot-bluetooth.service'], 'Unit', 'After'))
        for name in ('clock-coordinator.service', 'librescoot-bluetooth.service', 'dbc-dispatcher.service'):
            self.assertNotIn('network-online.target', words(units[name], 'Unit', 'After'))
            self.assertNotIn('network-online.target', words(units[name], 'Unit', 'Requires'))
        self.assertIn('--wait=3s', units['librescoot-bluetooth.service']['Service']['ExecStartPre'])
        for name in ('dbc-dispatcher.service', 'dbc-dispatcher-rpi4.service'):
            p = unit(ROOT / ('recipes-graphics/dbc-dispatcher/files/' + name))
            self.assertIn('--wait=10s', p['Service']['ExecStartPre'])
            self.assertNotIn('clock-coordinator.service', words(p, 'Unit', 'After'))


if __name__ == '__main__':
    unittest.main()
