# Releasing

Releases are made from a clean, reviewed tag. The release job is manually
dispatched and requires the protected `central-release` GitHub Environment.
Central publication remains manual (`autoPublish=false`) so a maintainer can
inspect validation results before publishing.

Configure these secrets on the protected `central-release` environment:
`CENTRAL_USERNAME`, `CENTRAL_TOKEN`, `MAVEN_GPG_PRIVATE_KEY` (ASCII-armored
private key), and `MAVEN_GPG_PASSPHRASE`. Keep the environment restricted to
release maintainers and rotate credentials independently of repository code.

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
5. Create and push the annotated `v<version>` tag. Dispatch the release
   workflow with that tag after two-person review.
6. Inspect Central validation, then use the Central Portal manual Publish
   action. Verify Maven metadata, Gradle resolution, and the generated-plugin
   goal from a clean consumer project.

Never publish snapshots, credentials, or the experimental `utils` and
`elasticsearch-sql` artifacts in the 0.1.x bundle. Rollbacks are handled by
publishing a corrected patch release; do not rewrite a published tag.
Apply and audit the protections in [docs/repository-settings.md](docs/repository-settings.md)
before releasing.
