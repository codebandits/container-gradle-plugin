#!/usr/bin/env sh

set -e

WRAPPER_PROPERTIES="./gradle/wrapper/gradle-wrapper.properties"
WRAPPER_JAR="./gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "gradle-wrapper.jar is missing, downloading..."
  DISTRIBUTION_URL=$(grep -o 'distributionUrl=.*' "$WRAPPER_PROPERTIES" | cut -d'=' -f2)
  GRADLE_VERSION=$(echo "$DISTRIBUTION_URL" | sed -nE 's/.*gradle-(.+)-(bin|all)\.zip/\1/p')
  if echo "$GRADLE_VERSION" | grep -qE '^[0-9]+\.[0-9]+$'; then
    GRADLE_VERSION="${GRADLE_VERSION}.0"
  fi
  JAR_URL="https://github.com/gradle/gradle/raw/refs/tags/v${GRADLE_VERSION}/gradle/wrapper/gradle-wrapper.jar"
  curl -s -L -o "$WRAPPER_JAR" "$JAR_URL"
  echo "gradle-wrapper.jar downloaded successfully."
fi
