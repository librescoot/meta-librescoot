import pathlib
import subprocess
import tempfile
import unittest

CLASS = pathlib.Path(__file__).resolve().parents[1] / "classes/librescoot-go.bbclass"


class GoReleaseVersionTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.repo = pathlib.Path(self.tmp.name)
        self.git("init", "-q")
        self.git("config", "user.email", "test@example.com")
        self.git("config", "user.name", "Test")
        (self.repo / "source").write_text("source\n")
        self.git("add", "source")
        self.git("commit", "-qm", "source")
        self.rev = self.git("rev-parse", "HEAD")
        self.git("tag", "-am", "release", "v1.2.3")
        text = CLASS.read_text()
        self.guard = text.split('    # An explicit release version', 1)[1].split('    # Save version for packaging', 1)[0]
        self.guard = '# An explicit release version' + self.guard

    def git(self, *args):
        return subprocess.check_output(["git", "-C", str(self.repo), *args], text=True).strip()

    def run_guard(self, version="v1.2.3", rev=None, repo=True):
        guard = self.guard.replace('${LIBRESCOOT_GO_RELEASE_VERSION}', version).replace('${SRCREV}', self.rev if rev is None else rev)
        script = 'bbfatal() { echo "$*" >&2; exit 1; }\nVERSION=original\nGITDIR="$1"\n' + guard + '\nprintf "%s" "$VERSION"\n'
        return subprocess.run(["bash", "-c", script, "guard", str(self.repo) if repo else ""], capture_output=True, text=True)

    def test_exact_annotated_tag(self):
        result = self.run_guard()
        self.assertEqual((result.returncode, result.stdout), (0, "v1.2.3"))

    def test_opt_out(self):
        result = self.run_guard(version="", rev="AUTOINC", repo=False)
        self.assertEqual((result.returncode, result.stdout), (0, "original"))

    def test_rejects_invalid_inputs(self):
        self.git("tag", "v1.2.4")
        for kwargs in ({"version": "v1.2.3-1-g123"}, {"version": "v01.2.3"}, {"version": "v1.2.4"}, {"version": "v1.2.5"}, {"rev": "AUTOINC"}, {"rev": "a" * 40}, {"repo": False}):
            with self.subTest(kwargs=kwargs):
                self.assertNotEqual(self.run_guard(**kwargs).returncode, 0)
        (self.repo / "source").write_text("modified\n")
        self.assertNotEqual(self.run_guard().returncode, 0)
        self.git("checkout", "--", "source")
        (self.repo / "untracked").write_text("dirty\n")
        self.assertNotEqual(self.run_guard().returncode, 0)

    def test_task_references_release_variable(self):
        self.assertIn('${LIBRESCOOT_GO_RELEASE_VERSION}', CLASS.read_text().split('do_compile() {', 1)[1].split('\n}', 1)[0])


if __name__ == "__main__":
    unittest.main()
