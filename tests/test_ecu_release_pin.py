"""Allow selected ECU release revisions without moving nightly off main."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]


class EcuReleasePinTests(unittest.TestCase):
    def test_pinned_revision_can_live_on_release_branch(self):
        recipe = (ROOT / 'recipes-core/ecu-service/ecu-service.bb').read_text()
        uri = re.search(r'^SRC_URI = "([^"]+)"', recipe, re.M)[1]
        parameters = dict(item.split('=', 1) for item in uri.split(';')[1:])
        self.assertEqual(parameters['nobranch'], '1')
        self.assertEqual(parameters['branch'], 'main')
        self.assertRegex(recipe, r'(?m)^SRCREV = "\$\{AUTOREV\}"$')


if __name__ == '__main__':
    unittest.main()
