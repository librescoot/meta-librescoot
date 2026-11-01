import getpass
import os
import pathlib
import socket
import subprocess
import tempfile
import time
import unittest


class ClockStartupTest(unittest.TestCase):
    @unittest.skipUnless(os.environ.get('CLOCK_COORDINATOR_TEST_BINARY'), 'set coordinator test binary')
    def test_missing_coordinator_wait_is_bounded(self):
        with tempfile.TemporaryDirectory(prefix='clock-missing-', dir='/tmp') as root:
            started = time.monotonic()
            subprocess.run([os.environ['CLOCK_COORDINATOR_TEST_BINARY'], '--wait=200ms', '--run-dir=' + root], check=True, timeout=2)
            self.assertLess(time.monotonic() - started, 1.5)


@unittest.skipUnless(os.environ.get('CHRONYD_TEST_BINARY') and os.environ.get('CHRONYC_TEST_BINARY'), 'set chronyd and chronyc test binaries')
class ChronydBootOrderTest(unittest.TestCase):
    def test_offline_holdover_in_both_boot_orders(self):
        for first in ('mdb', 'dbc'):
            with self.subTest(first=first), tempfile.TemporaryDirectory(prefix='clock-boards-', dir='/tmp') as root:
                root = pathlib.Path(root)
                with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
                    sock.bind(('127.0.0.1', 0))
                    port = sock.getsockname()[1]
                processes = []
                def start(board):
                    directory = root / board
                    directory.mkdir()
                    base = f'cmdport 0\nbindcmdaddress {directory}/cmd.sock\npidfile {directory}/pid\n'
                    if board == 'mdb':
                        base += f'port {port}\nbindaddress 127.0.0.1\nallow 127.0.0.1\nlocal stratum 10\n'
                    else:
                        base += f'port 0\nmaxdistance 8\nserver 127.0.0.1 port {port} iburst minpoll 0 maxpoll 0\n'
                    conf = directory / 'chrony.conf'
                    conf.write_text(base)
                    log = (directory / 'log').open('w')
                    process = subprocess.Popen([os.environ['CHRONYD_TEST_BINARY'], '-d', '-x', '-U', '-u', getpass.getuser(), '-f', str(conf)], stdout=log, stderr=log)
                    processes.append((process, log))
                    deadline = time.monotonic() + 3
                    while not (directory / 'cmd.sock').exists():
                        if process.poll() is not None or time.monotonic() > deadline:
                            self.fail(f'{board} failed to start: {(directory / "log").read_text()}')
                        time.sleep(.05)
                def query(*args):
                    return subprocess.check_output([os.environ['CHRONYC_TEST_BINARY'], '-h', str(root / 'dbc/cmd.sock'), '-n', *args], text=True, timeout=2)
                try:
                    start(first)
                    if first == 'dbc':
                        self.assertNotIn('^,*', query('-c', 'sources'))
                    start('dbc' if first == 'mdb' else 'mdb')
                    deadline = time.monotonic() + 12
                    while time.monotonic() < deadline:
                        if '^,*' in query('-c', 'sources'):
                            break
                        time.sleep(.2)
                    else:
                        self.fail('offline MDB source was not selected')
                    self.assertIn('7F7F0101', query('ntpdata', '127.0.0.1'))
                finally:
                    for process, log in processes:
                        process.terminate()
                        try:
                            process.wait(timeout=3)
                        except subprocess.TimeoutExpired:
                            process.kill()
                            process.wait()
                        log.close()


if __name__ == '__main__':
    unittest.main()
