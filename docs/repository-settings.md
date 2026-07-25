# Repository settings

Configure the default GitHub branch with these protections after publishing:

- Require pull requests and at least one approving review, including CODEOWNERS.
- Require all pull-request `CI` matrix jobs and `CodeQL` to pass. Review the
  scheduled OpenSSF Scorecard result as a repository security signal.
- Require the branch to be current before merge; dismiss stale approvals.
- Block force pushes and branch deletion; apply rules to administrators.
- Enable Dependabot alerts/security updates, secret scanning, and push protection.
- Restrict the `central-release` environment to release maintainers and require review.

Repository-host settings cannot be enforced by files in this checkout. Audit
them before every release and after ownership or organization-policy changes.
