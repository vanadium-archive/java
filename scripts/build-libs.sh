#!/bin/bash

if [[ $# -eq 0 ]]; then
  echo "Usage: $0 [OUTPUT FOLDER]"
  exit 1
fi
readonly DEST_DIR=$1
readonly NATIVE_DIR="${DEST_DIR}/armeabi-v7a"

source "${VEYRON_ROOT}/environment/scripts/lib/run.sh"

# Run the mobile setup.
run "${VEYRON_ROOT}/environment/scripts/setup/linux/mobile/setup.sh"

# Make the destination directory.
run mkdir -p "${DEST_DIR}"
run mkdir -p "${NATIVE_DIR}"

# Build the veyron android library.
readonly REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null)
readonly GOANDROID="${REPO_ROOT}/scripts/go-android"
GOPATH="${VEYRON_ROOT}/veyron.new/:${GOPATH}" run "${GOANDROID}" build -o "$NATIVE_DIR/libveyronjni.so" -v -ldflags="-android -shared -extld \"${VEYRON_ROOT}/environment/android/ndk-toolchain/bin/arm-linux-androideabi-gcc\" -extldflags '-march=armv7-a -mfloat-abi=softfp -mfpu=vfpv3-d16'" -tags android veyron.io/jni/runtimes/google

# Copy JNI Wrapper.
run cp "${VEYRON_ROOT}/environment/cout/jni-wrapper-1.0/android/lib/libjniwrapper.so" "${NATIVE_DIR}/libjniwrapper.so"

# Copy third party libraries.
readonly THIRD_PARTY_JAVA="${VEYRON_ROOT}/third_party/java"
run cp "${THIRD_PARTY_JAVA}/commons-math3-3.3/commons-math3-3.3.jar" "${DEST_DIR}"
run cp "${THIRD_PARTY_JAVA}/google-gson-2.2.4/gson-2.2.4.jar" "${DEST_DIR}"
run cp "${THIRD_PARTY_JAVA}/guava-17.0/guava-17.0.jar" "${DEST_DIR}"
run cp "${THIRD_PARTY_JAVA}/joda-time-2.3/joda-time-2.3.jar" "${DEST_DIR}"
