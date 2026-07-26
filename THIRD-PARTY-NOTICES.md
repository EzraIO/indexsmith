# Third-party notices

IndexSmith is distributed under the Apache License 2.0. Its build
also uses the following externally maintained components. Their licenses are
included in the corresponding artifacts and remain applicable to those
components:

- Apache Maven, Maven Plugin Tools, Commons Lang, Jackson, Guava, and
  Apache Antlr: Apache License 2.0.
- JavaParser: Apache License 2.0.
- Elasticsearch Java clients and REST libraries: Apache License 2.0.
- JUnit, SLF4J, Testcontainers, and XStream: their upstream licenses as
  declared in their published artifacts.

The experimental `indexsmith-sql` module includes generated parser output
from an Antlr grammar and is excluded from the 0.1.x Central publication
bundle. Consumers should review their resolved dependency tree and preserve
upstream notices when redistributing binaries.
