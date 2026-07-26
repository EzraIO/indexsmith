# Experimental SQL module

`indexsmith-sql` is a build-time experiment in 0.1.x. It parses a
restricted SQL grammar into an internal AST; it does not execute SQL, generate
production Elasticsearch JSON, enforce tenant filters, or provide an AI query
endpoint. `UPDATE` and `DELETE` remain unsupported.

The module and `indexsmith-utils` are excluded from the public BOM and
Central release bundle. The legacy ES7 `QueryBuilders` source helper is
deprecated. Applications should use `SearchQueryBuilder`, `SearchRequest`, and
an optional `RestrictedQueryPolicy` for production queries.

Promotion requires a version-neutral query plan, schema-aware validation,
parameter binding, explicit cost limits, ES7/ES8 compatibility tests, and a
separate public API review.
