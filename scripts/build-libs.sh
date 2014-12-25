#!/bin/bash

source "$(go list -f {{.Dir}} veyron.io/veyron/shell/lib)/shell.sh"

main() {
  if [[ "$#" -eq 0 ]]; then
    echo "Usage: $0 [OUTPUT FOLDER]"
    exit 1
  fi
  local -r DEST_DIR="$1"
  local -r NATIVE_DIR="${DEST_DIR}/armeabi-v7a"

  # Run the mobile setup.
  veyron profile setup mobile

  # Make the destination directory.
  mkdir -p "${DEST_DIR}"
  mkdir -p "${NATIVE_DIR}"

  # Make sure that no stale Go object files exist.
  veyron goext distclean

  # Build the veyron android library.
  unset GOROOT
  veyron xgo armv7-linux-android build -o "${NATIVE_DIR}/libveyronjni.so" -ldflags="-android -shared -extld \"${VANADIUM_ROOT}/environment/android/ndk-toolchain/bin/arm-linux-androideabi-gcc\" -extldflags '-march=armv7-a -mfloat-abi=softfp -mfpu=vfpv3-d16'" -tags android veyron.io/jni

  # Copy JNI Wrapper.
  cp "${VANADIUM_ROOT}/environment/cout/jni-wrapper-1.0/android/lib/libjniwrapper.so" "${NATIVE_DIR}/libjniwrapper.so"

  # Copy third party libraries.
  local -r THIRD_PARTY_JAVA="${VANADIUM_ROOT}/third_party/java"
  cp "${THIRD_PARTY_JAVA}/commons-math3-3.3/commons-math3-3.3.jar" "${DEST_DIR}"
  cp "${THIRD_PARTY_JAVA}/google-gson-2.2.4/gson-2.2.4.jar" "${DEST_DIR}"
  cp "${THIRD_PARTY_JAVA}/guava-17.0/guava-17.0.jar" "${DEST_DIR}"
  cp "${THIRD_PARTY_JAVA}/joda-time-2.3/joda-time-2.3.jar" "${DEST_DIR}"
  cp "${THIRD_PARTY_JAVA}/junit-r4.11/lib/hamcrest-core-1.3.jar" "${DEST_DIR}"
  cp "${THIRD_PARTY_JAVA}/junit-r4.11/lib/junit-4.11.jar" "${DEST_DIR}"
}

main "$@"
