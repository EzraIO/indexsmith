#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
allow_unsigned=false
if [[ "${1:-}" == "--allow-unsigned" ]]; then
  allow_unsigned=true
fi

version="$(${project_dir}/mvnw -q -DforceStdout help:evaluate -Dexpression=project.version)"
stable_modules=(
  indexsmith-client-core
  indexsmith-client-es7
  indexsmith-client-es8
  indexsmith-api
  indexsmith-generator
  indexsmith-maven-plugin
)
legacy_brand_pattern='easy''elasticsearch|Easy''Elasticsearch|easy-''elasticsearch'

require_file() {
  if [[ ! -s "$1" ]]; then
    echo "Missing release artifact: $1" >&2
    exit 1
  fi
}

require_signature() {
  if [[ "$allow_unsigned" == false ]]; then
    local artifact="$1"
    local signature="${artifact}.asc"
    require_file "$signature"
    if ! gpg --batch --verify "$signature" "$artifact" >/dev/null 2>&1; then
      echo "Invalid release signature: ${signature}" >&2
      exit 1
    fi
  fi
}

verify_pom_signatures() {
  local directory="$1"
  local label="$2"
  local found=false
  while IFS= read -r signature; do
    found=true
    local pom="${signature%.asc}"
    require_file "$pom"
    if ! gpg --batch --verify "$signature" "$pom" >/dev/null 2>&1; then
      echo "Invalid signed POM for ${label}: ${signature}" >&2
      exit 1
    fi
  done < <(find "$directory" -maxdepth 1 -name '*.pom.asc' -type f -print)
  if [[ "$found" == false ]]; then
    echo "Missing signed POM for ${label}" >&2
    exit 1
  fi
}

for module in "${stable_modules[@]}"; do
  artifact="$module"
  main_jar="${project_dir}/${module}/target/${artifact}-${version}.jar"
  sources_jar="${project_dir}/${module}/target/${artifact}-${version}-sources.jar"
  javadoc_jar="${project_dir}/${module}/target/${artifact}-${version}-javadoc.jar"
  require_file "$main_jar"
  require_file "$sources_jar"
  require_file "$javadoc_jar"
  require_signature "$main_jar"
  require_signature "$sources_jar"
  require_signature "$javadoc_jar"
  jar_entries="$(jar tf "$main_jar")"
  rg -q '^META-INF/LICENSE$' <<< "$jar_entries"
  rg -q '^META-INF/NOTICE$' <<< "$jar_entries"
  rg -q '^META-INF/THIRD-PARTY-NOTICES.md$' <<< "$jar_entries"
  if rg -q "$legacy_brand_pattern" <<< "$jar_entries"; then
    echo "Legacy brand entry found in ${main_jar}" >&2
    exit 1
  fi
done

if [[ -n "${API_BASELINE_VERSION:-}" ]]; then
  for module in "${stable_modules[@]}"; do
    require_file "${project_dir}/${module}/target/japicmp/check-public-api-compatibility.xml"
    require_file "${project_dir}/${module}/target/japicmp/check-public-api-compatibility.html"
  done
fi

provider_spi='META-INF/services/io.github.indexsmith.client.ElasticsearchClientProvider'
es7_entries="$(jar tf "${project_dir}/indexsmith-client-es7/target/indexsmith-client-es7-${version}.jar")"
es8_entries="$(jar tf "${project_dir}/indexsmith-client-es8/target/indexsmith-client-es8-${version}.jar")"
rg -q "^${provider_spi}$" <<< "$es7_entries"
rg -q "^${provider_spi}$" <<< "$es8_entries"
plugin_jar="${project_dir}/indexsmith-maven-plugin/target/indexsmith-maven-plugin-${version}.jar"
plugin_entries="$(jar tf "$plugin_jar")"
rg -q '^io/github/indexsmith/maven/plugin/HelpMojo.class$' <<< "$plugin_entries"
if rg -q 'indexsmith_maven_plugin' <<< "$plugin_entries"; then
  echo "Generated HelpMojo escaped the IndexSmith package root" >&2
  exit 1
fi

sbom_base="${project_dir}/target/indexsmith-${version}-cyclonedx"
require_file "${sbom_base}.json"
require_file "${sbom_base}.xml"
require_signature "${sbom_base}.json"
require_signature "${sbom_base}.xml"
buildinfo="${project_dir}/target/indexsmith-parent-${version}.buildinfo"
require_file "$buildinfo"
require_signature "$buildinfo"

if [[ "$allow_unsigned" == false ]]; then
  verify_pom_signatures "${project_dir}/target" "parent"
  for module in indexsmith-bom "${stable_modules[@]}"; do
    verify_pom_signatures "${project_dir}/${module}/target" "${module}"
  done
fi

echo "Release artifacts verified for ${version}"
