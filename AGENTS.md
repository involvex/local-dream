# Agent Instructions for Local Dream

## Build System

This project uses **two separate build systems** that must be run in sequence:

1. **Native C++** (CMake) → produces `libstable_diffusion_core.so` and QNN libs
2. **Android/Gradle** → packages native libs into the APK

**Order matters:** Native build must complete before Gradle build, otherwise the APK lacks the native library.

## Critical Dependencies

- **QNN SDK**: Required at `/data/qairt/2.39.0.250926`. CMake build fails without it. Edit `app/src/main/cpp/CMakeLists.txt:8` if your path differs.
- **Android NDK**: Expected at `/data/android-ndk-r28` (in `CMakePresets.json`). Uses `ccache` if available.
- **Java 17**: Required for Gradle (set in `build.gradle.kts`).
- **Git submodules**: Run `git submodule update --init --recursive` after cloning. Many C++ deps are submodules.

## Native Build

**Linux/macOS:**
```bash
cd app/src/main/cpp
./build.sh
```

**Windows (PowerShell):**
```powershell
cd app/src/main/cpp
.\build.bat
```

These scripts:
- Configure and build with CMake (preset: `android-release`)
- Copy QNN libraries from the SDK into `app/src/main/assets/qnnlibs/`
- Copy `libstable_diffusion_core.so` to `app/src/main/jniLibs/arm64-v8a/`

**Important:** The QNN SDK path is hardcoded in `CMakeLists.txt`. Also, `SampleApp.patch` is auto-applied during CMake configure to vendor code.

## Android Build

After native build completes:
```bash
# Debug builds
./gradlew assembleBasicDebug   # without NSFW filter
./gradlew assembleFilterDebug  # with NSFW filter

# Release (requires signing config in gradle.properties)
./gradlew assembleBasicRelease
```

APK output: `app/build/outputs/apk/<flavor>/debug/LocalDream_armv8a_<version>.apk`

Install: `adb install -r <apk-path>`

## Architecture Overview

- **Native binary** (`libstable_diffusion_core.so`) runs as a **separate process**, not JNI.
- `BackendService` launches the binary; it starts an **HTTP server on 127.0.0.1:8081** (cpp-httplib).
- Kotlin UI uses **OkHttp** to call endpoints: `/tokenize`, `/generate`, `/health`, `/upscale`.
- Two execution backends:
  - **NPU** (QNN): for supported Snapdragon chips; uses `libQnnHtp.so` from assets
  - **CPU** (MNN): fallback for unsupported devices or CPU-only models
- **Product flavors**:
  - `basic` - standard build
  - `filter` - includes NSFW safety checker (`safety_checker.mnn`)
- **ABI**: `arm64-v8a` only (see `app/build.gradle.kts:24`)

## Model System

Models stored in `filesDir/models/<modelId>/`. Required files vary by type:

- **NPU (QNN)**: `unet.bin`, `clip.bin`, `vae_decoder.bin`, `tokenizer.json`, optional `vae_encoder.bin`, `pos_emb.bin`, `token_emb.bin`, and resolution `.patch` files (zstd)
- **CPU (MNN)**: `unet.mnn`, `clip.mnn`, `vae_decoder.mnn`, `tokenizer.json`, optional `vae_encoder.mnn`
- **SDXL**: additionally requires `clip_2.mnn`, `pos_emb_2.bin`, `token_emb_2.bin`
- **Version marker**: NPU models contain a `v3` file to track version (for upgrade detection)

Model selection is **chipset-aware** (`Model.getChipsetSuffix()`). The correct variant (e.g., `..._8gen1.bin`, `..._min.bin`) is downloaded based on `Build.SOC_MODEL`. Custom models are detected via `finished` or `npucustom` marker files.

Models are downloaded from HuggingFace via `ModelDownloadService` (foreground service with progress notifications).

## Key Source Locations

- Entry: `app/src/main/java/io/github/xororz/localdream/MainActivity.kt`
- Backend launcher: `app/src/main/java/io/github/xororz/localdream/service/BackendService.kt`
- Model repo & chipset logic: `app/src/main/java/io/github/xororz/localdream/data/Model.kt`
- Native HTTP server: `app/src/main/cpp/src/main.cpp`
- Build config: `app/build.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts`
- Version catalog: `gradle/libs.versions.toml`

## Testing

```bash
# Unit tests (host JVM)
./gradlew test

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

No significant test coverage; both test classes are placeholder examples.

## Gotchas

- **QNN SDK path is hardcoded** - you must adjust `CMakeLists.txt:8` or create a symlink at `/data/qairt/2.39.0.250926`.
- **Native build doesn't auto-run** - modifying `app/src/main/cpp/` requires manual rebuild before Gradle picks up changes.
- **Submodules are not auto-cloned** - `git submodule update --init --recursive` is required after fresh clone.
- **Only arm64-v8a** - attempting to build for other ABIs will fail due to hardcoded filters and native deps.
- **Filter flavor assets** - `safety_checker.mnn` is not part of the repo; ensure it exists in `app/src/main/assets/` for filter builds.
- **Gradle properties for signing** - for release builds you need `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` (typically in `~/.gradle/gradle.properties` or project `gradle.properties`).
- **Clean rebuilds** - if native lib changes seem ignored, delete `app/src/main/jniLibs/` and `app/src/main/assets/qnnlibs/` then re-run native build script.
- **Port 8081 conflict** - the native backend binds to 127.0.0.1:8081; ensure it's free.
- **Storage permissions** - app requests `WRITE_EXTERNAL_STORAGE` (pre-Android 10) and `POST_NOTIFICATIONS` (Android 13+); testing on older/newer Android versions may behave differently.

## Not Recommended / Out of Scope

- No CI/CD, pre-commit hooks, lint/format configs beyond defaults. Add if needed.
- No custom Gradle tasks; stick to standard Android Gradle Plugin tasks.
- Don't attempt to modify vendor submodules directly; consider forking if changes needed.
