# GEMINI.md - Local Dream

## Project Overview
**Local Dream** is an Android application designed for high-performance Stable Diffusion inference directly on-device. It leverages Qualcomm's Snapdragon NPU acceleration via the QNN SDK and provides a CPU/GPU fallback using Alibaba's MNN framework. The project supports both SD1.5 and SDXL models.

## Tech Stack
- **Android Frontend:**
  - **Language:** Kotlin
  - **UI Framework:** Jetpack Compose
  - **Database:** Room
  - **Preferences:** DataStore
  - **Dependency Injection:** Manual/Local repositories (ModelRepository)
  - **Image Loading:** Coil
  - **Networking:** OkHttp (for downloads), Navigation Compose
- **Native Backend (Inference Engine):**
  - **Language:** C++17
  - **NPU Acceleration:** Qualcomm QNN SDK (AI Engine Direct)
  - **CPU/GPU Inference:** Alibaba MNN
  - **Tokenization:** mlc-ai/tokenizers-cpp
  - **Tensor Operations:** xtensor
  - **Communication:** yhirose/cpp-httplib (HTTP server on port 8081)
  - **Compression:** facebook/zstd (for models)

## Architecture
The application uses a "Sidecar" architecture:
1.  **Frontend (Android):** Manages models, UI, and the lifecycle of the `BackendService`.
2.  **Backend (Native):** `libstable_diffusion_core.so` is executed as a standalone process by the `BackendService`. It hosts an HTTP server that listens for generation requests from the frontend.
3.  **Runtime Management:** The `BackendService` prepares a runtime environment in the app's internal storage, copying necessary QNN libraries and setting executable permissions.

## Key Directories
- `app/src/main/java`: Android frontend source code.
  - `data/`: Room entities, DAOs, and repositories.
  - `service/`: `BackendService` (manages native process), `ModelDownloadService`, `BackgroundGenerationService`.
  - `ui/`: Compose screens and components.
- `app/src/main/cpp`: C++ backend source code.
  - `3rdparty/`: External libraries (MNN, QNN SampleApp, tokenizers-cpp, etc.).
  - `src/`: Core inference logic (PromptProcessor, QnnModel, Scheduler).
- `app/src/main/assets`: Static assets, including default configurations and QNN libraries.
- `gradle/libs.versions.toml`: Centralized dependency management.

## Building and Running
### Prerequisites
- Android Studio Ladybug+ or IntelliJ IDEA.
- Android NDK (defined in `build.gradle.kts` and `CMakeLists.txt`).
- Qualcomm QNN SDK (path can be set via `QNN_SDK_ROOT` environment variable or defaults to a path in `CMakeLists.txt`).

### Build Commands
- **Assemble Debug APK:** `./gradlew assembleDebug`
- **Assemble Release APK:** `./gradlew assembleRelease`
- **Lint Check:** `./gradlew lint`

### Native Build
The native library is automatically built as part of the Gradle process via the `externalNativeBuild` configuration in `app/build.gradle.kts`.

## Development Conventions
- **UI:** Use Jetpack Compose for all new UI components. Follow Material 3 design principles.
- **Asynchronous Work:** Use Kotlin Coroutines and Flow for handling asynchronous operations and data streams.
- **Native Integration:** Most native logic should stay within the C++ backend process. Communication with the frontend should be done via the internal HTTP API (port 8081).
- **Model Management:** Models are stored in the app's internal data directory. Use `ModelRepository` to interact with model metadata.
- **NPU Support:** When adding new models or features, ensure compatibility with QNN HTP (Hexagon Tensor Processor).

## Continuous Improvement
- **TODO:** Implement comprehensive unit and instrumentation tests for the `BackendService` and native communication layer.
- **TODO:** Optimize model download and verification processes.
- **TODO:** Enhance memory management in the native backend for better stability on low-RAM devices.
