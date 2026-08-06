# Releasing

Releases are made from a clean, reviewed tag. The release job is manually
dispatched and requires the protected `central-release` GitHub Environment.
Central publication remains manual (`autoPublish=false`) so a maintainer can
inspect validation results before publishing.

Before the first Central release, verify ownership of the
`io.github.ezraio` namespace in the Central Portal. Do not publish under
this group ID until the corresponding GitHub identity or organization is under
maintainer control and namespace verification has completed.

Configure these secrets on the protected `central-release` environment:
`CENTRAL_USERNAME`, `CENTRAL_TOKEN`, `MAVEN_GPG_PRIVATE_KEY` (ASCII-armored
private key), and `MAVEN_GPG_PASSPHRASE`. Keep the environment restricted to
signed `v*` tags and rotate credentials independently of repository code.
Configure the full primary-key fingerprint as the non-secret environment
variable `MAVEN_GPG_KEY_FINGERPRINT`. The workflow rejects imported keys that
do not match it.

Use a dedicated release signing key with a separately exportable signing
subkey. Publish the public key to `keyserver.ubuntu.com` and
`keys.openpgp.org`, complete any required email verification, and confirm that
the full fingerprint can be retrieved before creating a release tag. Never
place a private key, passphrase, or Portal token in a command-line argument,
repository file, issue, or workflow log.

1. Update `CHANGELOG.md`, the root version, and compatibility notes.
2. Run `./mvnw --batch-mode --no-transfer-progress clean verify` and the
   Docker-backed `-Pintegration-tests` profile.
3. For releases after 0.1.0, run the `api-compatibility` profile against the
   previous published version; binary/source incompatible changes block 0.1.x.
4. Run `./mvnw -Prelease clean verify` with signing credentials in a secure
   environment. Check sources, Javadocs, checksums, `.asc` signatures,
   CycloneDX JSON/XML SBOM, `buildinfo`, `LICENSE`, `NOTICE`, and third-party
   notices in every release artifact.
   CI runs `bash scripts/verify-release-artifacts.sh` before and after upload;
   the script cryptographically verifies every detached signature. Local
   unsigned dry-runs use the `--allow-unsigned` argument.
   CI records unsigned-artifact SHA-512 values with
   `scripts/hash-release-artifacts.sh write` and compares them after upload.
5. Create and push the signed annotated `v<version>` tag. For the current
   single-maintainer project, dispatch the protected release workflow only
   after all required CI and CodeQL checks pass. Require a second reviewer
   once another release maintainer is added.
6. Inspect Central validation, then use the Central Portal manual Publish
   action. Verify Maven metadata, Gradle resolution, and the generated-plugin
   goal from a clean consumer project.

The current repository has one maintainer. Pull requests, the stable
`release-gate`, CodeQL, signed tags, the protected environment, and manual
Central publication remain mandatory; the approval count stays at zero to
avoid an impossible self-approval requirement. Add required PR and environment
reviewers when a second release maintainer joins.

Never publish snapshots, credentials, or the experimental `indexsmith-utils` and
`indexsmith-sql` artifacts in the 0.1.x bundle. Rollbacks are handled by
publishing a corrected patch release; do not rewrite a published tag.
Apply and audit the protections in [docs/repository-settings.md](docs/repository-settings.md)
before releasing.
