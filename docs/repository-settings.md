# Repository settings

Configure the default GitHub branch with these protections before publishing:

- Require pull requests, resolved review threads, and stale-review dismissal.
  The current single-maintainer repository records ownership in CODEOWNERS but
  keeps the required approval count at zero so releases cannot deadlock.
- Require the stable `release-gate` CI job and `CodeQL` analysis to pass. Review the
  scheduled OpenSSF Scorecard result as a repository security signal.
- Require the branch to be current before merge; dismiss stale approvals.
- Block force pushes and branch deletion; apply rules to administrators.
- Enable Dependabot alerts/security updates, secret scanning, and push protection.
- Restrict the `central-release` environment to signed `v*` tags and release
  credentials. Central remains `autoPublish=false`, providing a separate manual
  publication checkpoint. Add a required environment reviewer when a second
  release maintainer joins the project.

Repository-host settings cannot be enforced by files in this checkout. Audit
them before every release and after ownership or organization-policy changes.
