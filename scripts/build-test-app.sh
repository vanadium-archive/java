#!/bin/bash

# Generates and builds an app that includes vanadium java source.
# This is used to verify that it is possible to compile all of the java source code.
#
# Usage: build_test_app.sh [OUTPUT_DIR]
# If the output directory is not specified, a temporary directory will
# be created and cleaned up upon exit.

source "$(go list -f {{.Dir}} v.io/core/shell/lib)/shell.sh"

main() {
  # Output a message if ant is not in the path.
  local -r RESULT=$(shell::check_result which ant &> /dev/null)
  if [[ "${RESULT}" -ne 0 ]]; then
    echo "ant must be installed and in your PATH"
    exit 1
  fi

  local -r DEFAULT_SDK_LOC="${VANADIUM_ROOT}/environment/android/android-sdk-linux"
  local -r SDK_LOC="${ANDROID_SDK_HOME-$DEFAULT_SDK_LOC}"
  local -r ANDROID_TOOL="${SDK_LOC}/tools/android"

  local -r OUTPUT_DIR="${1-$(shell::tmp_dir)}"
  local -r APP_TARGET_ID="1" # Warning! This is a per machine value.
  local -r APP_PROJECT_NAME="VanadiumBuildApp"
  local -r APP_ACTIVITY_NAME="VanadiumBuildActivity"
  local -r APP_PACKAGE_NAME="com.vanadium.testing.buildapp"

  local -r SCRIPT_DIR=$(cd "$(dirname "$0")" ; pwd -P)
  cd "${SCRIPT_DIR}"
  local -r JAVA_MAIN_SRC_DIR="${VANADIUM_ROOT}/release/java/src/main/java"
  local -r JAVA_VDL_SRC_DIR="${VANADIUM_ROOT}/release/java/src/vdl/java"

  set -e
  set +x
  "${ANDROID_TOOL}" create project --target "${APP_TARGET_ID}" --name "${APP_PROJECT_NAME}" --path "${OUTPUT_DIR}" --activity "${APP_ACTIVITY_NAME}" --package "${APP_PACKAGE_NAME}"
  "${VANADIUM_ROOT}/release/java/scripts/build-libs.sh" "${OUTPUT_DIR}/libs"

  diff -q "${VANADIUM_ROOT}/release/java/scripts/ExpectedAndroidManifest.xml" "${OUTPUT_DIR}/AndroidManifest.xml"
  cp "${VANADIUM_ROOT}/release/java/scripts/ReplacementAndroidManifest.xml" "${OUTPUT_DIR}/AndroidManifest.xml"

  rm -rf "${OUTPUT_DIR}/src"
  mkdir "${OUTPUT_DIR}/src"
  cp -r "${JAVA_MAIN_SRC_DIR}"/* "${JAVA_VDL_SRC_DIR}"/* "${OUTPUT_DIR}/src/"
  cd "${OUTPUT_DIR}"
  ant debug
}

main "$@"
