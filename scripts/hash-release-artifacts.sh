#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mode="${1:-}"
manifest="${2:-${project_dir}/target/release-artifacts.sha512}"

if [[ "$mode" != "write" && "$mode" != "verify" ]]; then
  echo "Usage: $0 <write|verify> [manifest]" >&2
  exit 2
fi

cd "$project_dir"
version="$(./mvnw -q -DforceStdout help:evaluate -Dexpression=project.version)"
stable_modules=(
  indexsmith-client-core
  indexsmith-client-es7
  indexsmith-client-es8
  indexsmith-api
  indexsmith-generator
  indexsmith-maven-plugin
)
artifacts=(
  "target/indexsmith-parent-${version}.pom"
  "target/indexsmith-${version}-cyclonedx.json"
  "target/indexsmith-${version}-cyclonedx.xml"
  "indexsmith-bom/target/indexsmith-bom-${version}.pom"
)

for module in "${stable_modules[@]}"; do
  artifacts+=(
    "${module}/target/${module}-${version}.pom"
    "${module}/target/${module}-${version}.jar"
    "${module}/target/${module}-${version}-sources.jar"
    "${module}/target/${module}-${version}-javadoc.jar"
  )
done
artifacts+=(
  "indexsmith-maven-plugin/target/indexsmith-maven-plugin-${version}.buildinfo"
)

hash_file() {
  if command -v sha512sum >/dev/null 2>&1; then
    sha512sum "$1" | awk '{print $1}'
  else
    shasum -a 512 "$1" | awk '{print $1}'
  fi
}

if [[ "$mode" == "write" ]]; then
  mkdir -p "$(dirname "$manifest")"
  : > "$manifest"
  for artifact in "${artifacts[@]}"; do
    if [[ ! -s "$artifact" ]]; then
      echo "Missing release artifact for hash manifest: $artifact" >&2
      exit 1
    fi
    printf '%s  %s\n' "$(hash_file "$artifact")" "$artifact" >> "$manifest"
  done
  echo "Release artifact hashes written to ${manifest}"
  exit 0
fi

if [[ ! -s "$manifest" ]]; then
  echo "Missing release hash manifest: $manifest" >&2
  exit 1
fi

expected_entries="${#artifacts[@]}"
actual_entries="$(wc -l < "$manifest" | tr -d ' ')"
if [[ "$actual_entries" != "$expected_entries" ]]; then
  echo "Release hash manifest entry count changed: expected ${expected_entries}, found ${actual_entries}" >&2
  exit 1
fi

while read -r expected artifact; do
  if [[ ! -s "$artifact" ]]; then
    echo "Missing release artifact during hash verification: $artifact" >&2
    exit 1
  fi
  actual="$(hash_file "$artifact")"
  if [[ "$actual" != "$expected" ]]; then
    echo "Release artifact changed after pre-upload audit: $artifact" >&2
    exit 1
  fi
done < "$manifest"

echo "Release artifact hashes match ${manifest}"
