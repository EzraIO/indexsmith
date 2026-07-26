# Roadmap notes

These are exploratory ideas, not release commitments. Priorities are
evaluated against compatibility, testability, and maintenance cost.

- Broaden mapping coverage for uncommon Elasticsearch field types.
- Add explicit enum/type-handler mapping and configurable primitive/wrapper
  generation.
- Provide annotation-based examples after the generated repository contract
  is stable.
- Promote SQL and JavaParser utilities only after a version-neutral query
  plan, parameter binding, query-cost controls, independent APIs, legal review,
  coverage, and ES7/ES8 compatibility tests are complete.
- Add a Gradle-native generation task only if it can reuse the shared
  generator without duplicating Maven-plugin behavior.
