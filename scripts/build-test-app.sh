#!/bin/bash

# Generates and builds an app that includes Veyron's java source.
# This is used to verify that it is possible to compile all of the java source code.
#
# Usage: build_test_app.sh [OUTPUT_DIR]
# If the output directory is not specified, a temporary directory will
# be created and cleaned up upon exit.

source "${VEYRON_ROOT}/environment/scripts/lib/shell.sh"

main() {
  # Output a message if ant is not in the path.
  local -r RESULT=$(shell::check_result which ant &> /dev/null)
  if [[ "${RESULT}" -ne 0 ]]; then
    echo "ant must be installed and in your PATH"
    exit 1
  fi

  local -r DEFAULT_SDK_LOC="${VEYRON_ROOT}/environment/android/android-sdk-linux"
  local -r SDK_LOC="${ANDROID_SDK_HOME-$DEFAULT_SDK_LOC}"
  local -r ANDROID_TOOL="${SDK_LOC}/tools/android"

  local -r OUTPUT_DIR="${1-$(shell::tmp_dir)}"
  local -r APP_TARGET_ID="1" # Warning! This is a per machine value.
  local -r APP_PROJECT_NAME="VeyronBuildApp"
  local -r APP_ACTIVITY_NAME="VeyronBuildActivity"
  local -r APP_PACKAGE_NAME="com.veyron.testing.buildapp"

  local -r SCRIPT_DIR=$(cd "$(dirname "$0")" ; pwd -P)
  cd "${SCRIPT_DIR}"
  local -r JAVA_SRC_DIR="${VEYRON_ROOT}/veyron.new/java/src/main/java"

  set -e
  set +x
  "${ANDROID_TOOL}" create project --target "${APP_TARGET_ID}" --name "${APP_PROJECT_NAME}" --path "${OUTPUT_DIR}" --activity "${APP_ACTIVITY_NAME}" --package "${APP_PACKAGE_NAME}"
  "${VEYRON_ROOT}/veyron.new/java/scripts/build-libs.sh" "${OUTPUT_DIR}/libs"

  diff -q "${VEYRON_ROOT}/veyron.new/java/scripts/ExpectedAndroidManifest.xml" "${OUTPUT_DIR}/AndroidManifest.xml"
  cp "${VEYRON_ROOT}/veyron.new/java/scripts/ReplacementAndroidManifest.xml" "${OUTPUT_DIR}/AndroidManifest.xml"

  rm -r "${OUTPUT_DIR}/src"
  cd "${OUTPUT_DIR}"
  ln -s "${JAVA_SRC_DIR}" "src"
  ant debug
}

main "$@"
