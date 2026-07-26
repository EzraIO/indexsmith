# Security policy

## Supported versions

Only the latest released line receives security fixes. The 0.1.x line is
supported while it is the current release line; experimental modules may have
different readiness and are excluded from the Central publication bundle.

## Reporting a vulnerability

Do not open a public issue for a suspected vulnerability. Use GitHub's private
vulnerability reporting form under the repository's **Security** tab. Include
the affected coordinates and versions, reproduction steps, and a proposed
mitigation if available.
Please avoid including production credentials or personal data. We will
acknowledge receipt within seven days, coordinate a fix and disclosure date,
and credit reporters who want attribution.

## Safe configuration

Use TLS endpoints and environment placeholders for credentials. Do not commit
passwords, API keys, certificates, or generated files containing secrets.
Basic Auth and API Key configuration are mutually exclusive. Custom TLS uses a
standard `SSLContext`; hostname verification remains enabled. Request listener
events intentionally omit request/response bodies and authentication data.
The generator rejects XML `DOCTYPE`/`ENTITY` declarations and only accepts
relative output directories inside the Maven project.
