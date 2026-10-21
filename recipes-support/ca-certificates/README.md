# Compromised CA distrust

The `ca-certificates` append removes these generated Mozilla root certificates
before installation:

| Root | SHA-256 certificate fingerprint |
| --- | --- |
| emSign ECC Root CA - G3 | `86a1ecba089c4a8d3bbe2734c612ba341d813e043cf9e8a862cd5c57a36bbe6b` |
| AAA Certificate Services | `d7a7a0fb5d7e2731d771e9484ebcdef71d5f0c3e0a2948782bc83ee0ea699ef4` |

These roots provide the two public TLS trust paths to the compromised eMudhra
intermediates described in [Mozilla's incident report][incident]. Removing only
the eMudhra root leaves the Sectigo cross-sign path usable in older stores that
still include AAA Certificate Services.

Pruning happens after certificate generation and before any installation or
bundle generation. It applies to target, native and SDK variants. Both MDB and
DBC images receive the change through the shared package. The removed roots
cannot be reintroduced by `update-ca-certificates --default`: their source files
are not installed. Unrelated roots and locally supplied certificates are
unchanged. This is a firmware/package change, not a runtime revocation service.

## Tests

Requires Python 3, OpenSSL 3 and `update-ca-certificates`. No network access,
root privileges or changes to the host trust store are needed:

```sh
python3 recipes-support/ca-certificates/tests/test-trust.py -v
```

Set `UPDATE_CA_CERTIFICATES` to exercise a target build's update script.
The tests execute the recipe's pruning hook, assemble temporary stores and run
the real update script. They verify that:

- All twelve published incident TLS chains verify before pruning, through both
  the PEM bundle and OpenSSL's hashed certificate directory.
- All twelve fail with a missing issuer after pruning, including after a fresh
  `--default` rebuild. Verification uses a fixed date within the fixture leaf
  certificates' validity, so expiration cannot conceal a regression.
- An unrelated, freshly generated TLS certificate remains trusted.
- Already-removed roots and repeated pruning are handled normally.

Also build the actual recipe and native variant in a configured Yocto tree:

```sh
bitbake ca-certificates ca-certificates-native
```

## Fixture provenance

`tests/fixtures/` contains public certificates only, with no private keys.
They were retrieved on 2026-10-21 from the [incident response site][response]:

- The twelve PEM chains come from [`chains.zip`][chains], SHA-256
  `8a2edc81b6c51e3ad27d45ca1e441ec84c961e2dee0fc547f2a02c0f74315f95`.
- The two root certificates come from [`roots.json`][roots]. Their DER
  fingerprints are checked against the table above.

[incident]: https://bugzilla.mozilla.org/show_bug.cgi?id=2083449
[response]: https://emudhra.we-have-a-problem.com/
[chains]: https://emudhra.we-have-a-problem.com/chains.zip
[roots]: https://emudhra.we-have-a-problem.com/roots.json
