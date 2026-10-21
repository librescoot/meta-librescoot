#!/usr/bin/env python3
"""Test CA pruning and generated bundle/directory trust without network access."""
import base64
from datetime import datetime, timezone
import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import unittest

HERE = Path(__file__).resolve().parent
RECIPE = HERE.parent / "ca-certificates_%.bbappend"
FIXTURES = HERE / "fixtures"
ROOTS = {
    "emSign_ECC_Root_CA_-_G3.crt": "86a1ecba089c4a8d3bbe2734c612ba341d813e043cf9e8a862cd5c57a36bbe6b",
    "Comodo_AAA_Services_root.crt": "d7a7a0fb5d7e2731d771e9484ebcdef71d5f0c3e0a2948782bc83ee0ea699ef4",
}
# Keep expired test leaves from turning a trust regression into a false pass.
INCIDENT_TIME = int(datetime(2026, 10, 21, tzinfo=timezone.utc).timestamp())
PEM = re.compile(r"-----BEGIN CERTIFICATE-----.*?-----END CERTIFICATE-----", re.S)


def run(*args, check=True, **kwargs):
    return subprocess.run(list(map(str, args)), check=check, text=True,
                          capture_output=True, **kwargs)


def fingerprint(pem):
    body = pem.replace("-----BEGIN CERTIFICATE-----", "").replace("-----END CERTIFICATE-----", "")
    return hashlib.sha256(base64.b64decode(body)).hexdigest()


class TrustTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.update = os.environ.get("UPDATE_CA_CERTIFICATES", "/usr/sbin/update-ca-certificates")
        if not shutil.which("openssl") or not Path(cls.update).is_file():
            raise RuntimeError("Install openssl and ca-certificates, or set UPDATE_CA_CERTIFICATES")
        cls.temp = tempfile.TemporaryDirectory(prefix="librescoot-ca-control-")
        cls.addClassCleanup(cls.temp.cleanup)
        cls.control = Path(cls.temp.name)
        run("openssl", "req", "-x509", "-newkey", "ec", "-pkeyopt", "ec_paramgen_curve:P-256",
            "-nodes", "-subj", "/CN=Librescoot test root", "-days", "1",
            "-addext", "basicConstraints=critical,CA:TRUE",
            "-addext", "keyUsage=critical,keyCertSign,cRLSign",
            "-keyout", cls.control / "root.key", "-out", cls.control / "root.crt")
        run("openssl", "req", "-new", "-newkey", "ec", "-pkeyopt", "ec_paramgen_curve:P-256",
            "-nodes", "-subj", "/CN=healthy.example", "-keyout", cls.control / "leaf.key",
            "-out", cls.control / "leaf.csr")
        (cls.control / "leaf.ext").write_text(
            "subjectAltName=DNS:healthy.example\nextendedKeyUsage=serverAuth\n"
            "basicConstraints=critical,CA:FALSE\nkeyUsage=critical,digitalSignature\n")
        run("openssl", "x509", "-req", "-in", cls.control / "leaf.csr", "-days", "1",
            "-CA", cls.control / "root.crt", "-CAkey", cls.control / "root.key",
            "-set_serial", "1", "-extfile", cls.control / "leaf.ext", "-out", cls.control / "leaf.pem")

    def setUp(self):
        temp = tempfile.TemporaryDirectory(prefix="librescoot-ca-test-")
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.build = self.root / "build"
        self.mozilla = self.build / "mozilla"
        self.mozilla.mkdir(parents=True)
        for name in ROOTS:
            shutil.copyfile(FIXTURES / name, self.mozilla / name)
        shutil.copyfile(self.control / "root.crt", self.mozilla / "healthy.crt")

    def prune(self):
        # Execute the recipe's actual hook, not a duplicate implementation.
        hook = re.search(r"^do_compile:append\(\) \{\n(.*?)^\}", RECIPE.read_text(), re.M | re.S)
        self.assertIsNotNone(hook)
        run("sh", "-eu", "-c", hook[1], env={**os.environ, "B": str(self.build)})

    def install(self, name):
        dest = self.root / name
        source = dest / "usr/share/ca-certificates/mozilla"
        source.mkdir(parents=True)
        for cert in self.mozilla.glob("*.crt"):
            shutil.copyfile(cert, source / cert.name)
        (dest / "etc/ssl/certs").mkdir(parents=True)
        (dest / "etc/ca-certificates.conf").write_text("".join(
            f"mozilla/{cert.name}\n" for cert in sorted(source.glob("*.crt"))))
        self.update_store(dest)
        return dest

    def update_store(self, dest, *options):
        if "--sysroot)" in Path(self.update).read_text():
            run("sh", self.update, "--sysroot", dest, *options)
            return
        run("sh", self.update,
            "--certsconf", dest / "etc/ca-certificates.conf",
            "--certsdir", dest / "usr/share/ca-certificates",
            "--localcertsdir", dest / "usr/local/share/ca-certificates",
            "--etccertsdir", dest / "etc/ssl/certs",
            "--hooksdir", dest / "empty-hooks", *options)

    def verify(self, dest, chain, host, directory=False, incident=True):
        certs = PEM.findall(chain.read_text())
        leaf = self.root / "leaf.pem"
        intermediates = self.root / "intermediates.pem"
        leaf.write_text(certs[0] + "\n")
        intermediates.write_text("\n".join(certs[1:]) + "\n")
        certdir = dest / "etc/ssl/certs"
        args = ["openssl", "verify", "-no-CAstore", "-purpose", "sslserver", "-verify_hostname", host]
        if directory:
            args += ["-no-CAfile", "-CApath", certdir]
        else:
            args += ["-no-CApath", "-CAfile", certdir / "ca-certificates.crt"]
        if incident:
            args += ["-attime", INCIDENT_TIME]
        if len(certs) > 1:
            args += ["-untrusted", intermediates]
        return run(*args, leaf, check=False)

    def assert_trust(self, dest, compromised):
        chains = sorted(FIXTURES.glob("*.pem"))
        self.assertEqual(len(chains), 12)
        for directory in (False, True):
            for chain in chains:
                with self.subTest(chain=chain.name, directory=directory, compromised=compromised):
                    result = self.verify(dest, chain, chain.stem, directory)
                    if compromised:
                        self.assertEqual(result.returncode, 0, result.stderr)
                    else:
                        self.assertNotEqual(result.returncode, 0, result.stdout)
                        self.assertIn("unable to get local issuer certificate", result.stderr)
            result = self.verify(dest, self.control / "leaf.pem", "healthy.example", directory, incident=False)
            self.assertEqual(result.returncode, 0, result.stderr)

    def test_incident_fixture_fingerprints(self):
        for name, expected in ROOTS.items():
            self.assertEqual(fingerprint((FIXTURES / name).read_text()), expected)

    def test_both_trust_paths_are_removed(self):
        self.assert_trust(self.install("before"), compromised=True)
        self.prune()
        self.assertEqual(sorted(p.name for p in self.mozilla.iterdir()), ["healthy.crt"])
        after = self.install("after")
        self.assert_trust(after, compromised=False)
        for name in ROOTS:
            self.assertFalse((after / "etc/ssl/certs" / (Path(name).stem + ".pem")).exists())
        # Neither a fresh rehash nor --default can restore removed source roots.
        self.update_store(after, "--fresh", "--default")
        self.assert_trust(after, compromised=False)

    def test_roots_already_removed_upstream(self):
        for absent in ("Comodo_AAA_Services_root.crt", "emSign_ECC_Root_CA_-_G3.crt"):
            with self.subTest(absent=absent):
                for name in ROOTS:
                    shutil.copyfile(FIXTURES / name, self.mozilla / name)
                (self.mozilla / absent).unlink()
                self.prune()
                self.prune()
                self.assertEqual(sorted(p.name for p in self.mozilla.iterdir()), ["healthy.crt"])


if __name__ == "__main__":
    unittest.main()
