#!/bin/bash

# Runs java-language veyron tests.
#
# Usage: test.sh [OUTPUT_DIR]
# If the output directory is not specified, a temporary directory will
# be created and clean up upon exit.

source "${VEYRON_ROOT}/scripts/lib/shell.sh"

main() {
  # Output a message if ant is not in the path.
  local RESULT=$(shell::check_result which ant &> /dev/null)
  if [[ "${RESULT}" -ne 0 ]]; then
    echo "ant must be installed and in your PATH"
    exit 1
  fi

  local -r DEFAULT_SDK_LOC="${VEYRON_ROOT}/environment/android/android-sdk-linux"
  local -r SDK_LOC="${ANDROID_SDK_HOME-$DEFAULT_SDK_LOC}"
  local -r ANDROID_TOOL="${SDK_LOC}/tools/android"

  local -r OUTPUT_DIR="${1-$(shell::tmp_dir)}"
  local -r APP_DIR="${OUTPUT_DIR}/app"
  local -r TEST_DIR="${OUTPUT_DIR}/test"
  if [[ -e "${APP_DIR}" ]]; then
    echo "Directory ${APP_DIR} already exists."
    exit 1
  fi
  if [[ -e "${TEST_DIR}" ]]; then
    echo "Directory ${TEST_DIR} already exists."
    exit 1
  fi
  local -r TEST_PROJECT_NAME="test"
  local -r SCRIPT_DIR=$(cd "$(dirname "$0")" ; pwd -P)
  cd "${SCRIPT_DIR}"
  local -r JAVA_SRC_DIR="${VEYRON_ROOT}/veyron/java/src/test/java"

  set -e
  set +x
  "${VEYRON_ROOT}/veyron/java/scripts/build-test-app.sh" "${APP_DIR}"

  "${ANDROID_TOOL}" create test-project --name "${TEST_PROJECT_NAME}" --path "${TEST_DIR}" --main "${APP_DIR}"
  rm -r "${TEST_DIR}/src"
  cd "${TEST_DIR}"
  ln -s "${JAVA_SRC_DIR}" "src"
  set -x
  set +e

  local -r TMP_FILE=$(shell::tmp_file)
  # ant does not output an exit code on android test projects so we
  # need to grep the output.
  set -e
  set +x
  ant debug install test | tee "${TMP_FILE}"
  set -x
  set +e

  RESULT=$(shell::check_result grep "\[exec\] OK [(][0-9][0-9]* test" "${TMP_FILE}" &> /dev/null)
  if [[ "${RESULT}" -eq 0 ]]; then
    echo "PASS"
    exit 0
  else
    echo "FAIL"
    exit 1
  fi
}

main "$@"
