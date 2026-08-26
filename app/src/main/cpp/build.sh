set -e

# Locate an NDK: prefer ANDROID_NDK_ROOT, else the newest under the default
# SDK location (sdkmanager-managed).
if [ -z "${ANDROID_NDK_ROOT}" ]; then
    ANDROID_NDK_ROOT=$(ls -1d "${HOME}/Android/Sdk/ndk/"* 2>/dev/null | sort -V | tail -n1 || true)
fi
if [ -z "${ANDROID_NDK_ROOT}" ]; then
    echo "No NDK found. Set ANDROID_NDK_ROOT or install one via sdkmanager." >&2
    exit 1
fi
echo "Using NDK: ${ANDROID_NDK_ROOT}"
export ANDROID_NDK_ROOT

QNN_ARGS=()
if [ -n "${QNN_SDK_ROOT}" ]; then
    QNN_ARGS=(-DQNN_SDK_ROOT="${QNN_SDK_ROOT}")
fi

CMAKE_POLICY_VERSION_MINIMUM=3.5 cmake --preset android-release "${QNN_ARGS[@]}"
cmake --build --preset android-release

mkdir -p lib
cp -r ./build/android/qnnlibs ../assets/
mkdir -p ../jniLibs/arm64-v8a/
cp ./build/android/bin/arm64-v8a/libstable_diffusion_core.so ../jniLibs/arm64-v8a/

# LLM JNI library for the chat feature. Built from cpp/llm explicitly:
# CMake presets cannot retarget the source directory, so a preset would
# wrongly configure the root project.
(
    cd llm
    CMAKE_POLICY_VERSION_MINIMUM=3.5 cmake -B build -G Ninja \
        -DCMAKE_BUILD_TYPE=Release \
        -DANDROID_ABI=arm64-v8a \
        -DANDROID_PLATFORM=android-21 \
        -DCMAKE_TOOLCHAIN_FILE="${ANDROID_NDK_ROOT}/build/cmake/android.toolchain.cmake"
    cmake --build build
)
cp ./llm/build/obj/local/arm64-v8a/liblocaldream_llm.so ../jniLibs/arm64-v8a/
