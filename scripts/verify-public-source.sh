#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
legacy_identity='outa''keshi'
restricted_company_pattern='sank''uai|mei''tuan|dian''ping|美''团|大众''点评|三''快'
blocked_pattern="${legacy_identity}|${restricted_company_pattern}"
retired_maven_coordinate_pattern='io\.github\.indexsmith:indexsmith-|<groupId>io\.github\.indexsmith</groupId>'
forbidden_java_namespace_pattern='(^|[^A-Za-z0-9_.])io\.github\.ezr''aio(\.|;)|io/github/ezr''aio'
required_modules=(
  indexsmith-bom
  indexsmith-client-core
  indexsmith-client-es7
  indexsmith-client-es8
  indexsmith-api
  indexsmith-generator
  indexsmith-maven-plugin
  indexsmith-utils
  indexsmith-sql
)

cd "$project_dir"

if git grep -nI -i -E "$blocked_pattern" -- .; then
  echo "Blocked legacy identity or company marker found in tracked content." >&2
  exit 1
fi

if git ls-files | rg -i "$blocked_pattern"; then
  echo "Blocked legacy identity or company marker found in a tracked path." >&2
  exit 1
fi

if git grep -nI -E "$retired_maven_coordinate_pattern" -- \
    '*.xml' '*.md' '*.kts' '*.gradle' '*.properties' '*.yml' '*.yaml'; then
  echo "Retired io.github.indexsmith Maven coordinate found in tracked content." >&2
  exit 1
fi

if git grep -nI -E "$forbidden_java_namespace_pattern" -- '*/src/**' \
    || git ls-files '*/src/**' | rg 'io/github/ezr''aio' \
    || git grep -nI -E '(<automatic.module.name>|<japicmp.package.include>)io\.github\.ezr''aio' -- '*pom.xml'; then
  echo "Maven coordinates must not migrate the io.github.indexsmith Java namespace." >&2
  exit 1
fi

for module in "${required_modules[@]}"; do
  if [[ ! -f "${module}/pom.xml" ]]; then
    echo "Missing normalized module directory: ${module}" >&2
    exit 1
  fi
done

echo "Tracked source identity and module layout checks passed."
