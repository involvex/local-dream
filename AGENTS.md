# Agents Instructions for Local Dream

This file provides comprehensive instructions for AI agents working on the **Local Dream** project — an Android Stable Diffusion app with Snapdragon NPU acceleration.

---

## Project Overview

**Local Dream** is an open-source Android application that runs Stable Diffusion inference locally on mobile devices. It supports:

- **SD1.5** and **SDXL** models
- **Snapdragon NPU** acceleration (Hexagon V68+ for SD1.5, Snapdragon 8 Gen 3+ for SDXL)
- **CPU/GPU** inference fallback via MNN
- **Image upscaling** (Real-ESRGAN, UltraSharpV2)
- **Inpainting** capabilities
- **Remote hosting** (device-to-device inference)

---

## Technologies

### Android (Kotlin)
- **Language:** Kotlin (2.3.21)
- **UI Framework:** Jetpack Compose with Material 3
- **Architecture:** Single-module app (`:app`)
- **Min SDK:** 28 (Android 9)
- **Target SDK:** 36
- **Compile SDK:** 37
- **Java Compatibility:** 17
- **ABI:** arm64-v8a only

### Native (C++)
- **Standard:** C++17
- **Build System:** CMake (3.18+) with Ninja
- **NDK:** r28
- **Compiler:** Clang (via Android NDK toolchain)
- **Optimization:** -O3, -fno-rtti, -fPIC, hidden visibility

### Key Libraries

| Category | Library | Purpose |
|----------|---------|---------|
| NPU | Qualcomm QNN SDK (2.39.0) | Snapdragon NPU model execution |
| CPU/GPU | MNN (Alibaba) | CPU/OpenCL model inference |
| Tensor | xtensor-stack | Tensor operations & scheduling |
| Tokenizer | tokenizers-cpp | Text tokenization |
| HTTP | cpp-httplib | Local HTTP server |
| Image | stb_image | Image processing |
| Compression | zstd | Model compression |
| JSON | nlohmann/json | JSON parsing |
| Network | OkHttp | HTTP client |
| Images | Coil | Image loading |
| UI | Material 3 Compose | Design system |
| Database | Room | Local storage |
| Paging | Paging 3 | List pagination |

---

## Useful Commands

### Build Commands

#### Full APK Build (via Android Studio / Gradle)
```bash
# Debug build
.\gradlew.bat assembleBasicDebug

# Release build
.\gradlew.bat assembleBasicRelease

# Build with filter flavor
.\gradlew.bat assembleFilterRelease
```

#### Native C++ Build
```bash
# Navigate to C++ directory
cd app\src\main\cpp

# Configure (Android Release)
cmake --preset android-release -DCMAKE_POLICY_VERSION_MINIMUM=3.5

# Build
cmake --build --preset android-release

# The build script handles copy to jniLibs automatically
.\build.bat
```

#### Linux/macOS Native Build
```bash
cd app/src/main/cpp
chmod +x build.sh
./build.sh
```

### Code Quality Commands

#### ktlint (Formatting)
```bash
# Check formatting
.\gradlew.bat ktlintCheck

# Auto-fix formatting
.\gradlew.bat ktlintFormat
```

#### detekt (Static Analysis)
```bash
# Run detekt
.\gradlew.bat detekt

# Generate baseline (after fixing existing issues)
.\gradlew.bat detektBaseline
```

### Testing
```bash
# Unit tests
.\gradlew.bat test

# Instrumented tests (requires connected device/emulator)
.\gradlew.bat connectedAndroidTest
```

### Clean & Rebuild
```bash
# Clean all builds
.\gradlew.bat clean

# Clean and rebuild
.\gradlew.bat clean assembleBasicDebug
```

---

## Project Structure

```
local-dream/
├── app/
│   ├── build.gradle.kts          # App-level build config
│   ├── detekt.yml                # Static analysis config
│   ├── proguard-rules.pro        # ProGuard/R8 rules
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/               # Model files, QNN libs
│       ├── cpp/                  # Native C++ code
│       │   ├── CMakeLists.txt    # CMake build config
│       │   ├── CMakePresets.json # Build presets
│       │   ├── build.sh/.bat     # Build scripts
│       │   ├── src/              # C++ source files
│       │   └── 3rdparty/         # Git submodules
│       ├── java/io/github/xororz/localdream/
│       │   ├── MainActivity.kt
│       │   ├── LocalDreamApplication.kt
│       │   ├── data/             # Data layer (Room, preferences)
│       │   ├── navigation/       # Compose navigation
│       │   ├── remote/           # Remote hosting protocol
│       │   ├── service/          # Android services
│       │   ├── ui/               # Compose UI
│       │   │   ├── components/   # Reusable composables
│       │   │   ├── screens/      # Screen composables
│       │   │   └── theme/        # Material 3 theme
│       │   └── utils/            # Utility functions
│       └── res/                  # Android resources
├── build.gradle.kts              # Root build config
├── settings.gradle.kts           # Module settings
├── gradle.properties             # Gradle properties
├── gradle/libs.versions.toml     # Version catalog
├── .editorconfig                 # Code style rules
├── .gitmodules                   # Git submodule config
└── README.md
```

---

## Best Practices & Guidelines

### Kotlin / Compose Code Style

1. **Formatting:** Follow ktlint rules with `android_studio` code style
   - Max line length: 120 characters
   - Trailing commas allowed
   - Wildcard imports allowed for Compose icons/material3

2. **Naming Conventions:**
   - Composable functions: PascalCase (e.g., `ModelRunScreen`)
   - Regular functions: camelCase
   - Constants: SCREAMING_SNAKE_CASE or PascalCase for design tokens
   - Private variables: `_camelCase` or `camelCase`

3. **Compose Guidelines:**
   - Use `@Composable` annotation for all UI functions
   - Pass `modifier` as the first optional parameter
   - Keep composables focused and reusable
   - Use Material 3 components and theming

4. **Architecture:**
   - UI layer: `ui/screens/` for screens, `ui/components/` for reusable components
   - Data layer: `data/` for models, repositories, database
   - Services: `service/` for Android services
   - Navigation: `navigation/` for route definitions

### C++ Code Style

1. **Standards:** C++17 with -O3 optimization
2. **Naming:** PascalCase for classes/functions, camelCase for variables
3. **Headers:** Use `.hpp` for C++ headers
4. **Visibility:** Hidden by default, export only JNI entry points
5. **Memory:** Use RAII, avoid raw pointers where possible

### Git Workflow

1. **Commits:** Use conventional commit messages
   - `feat:` for new features
   - `fix:` for bug fixes
   - `refactor:` for code refactoring
   - `docs:` for documentation
   - `chore:` for maintenance

2. **Branches:**
   - `main` - stable release branch
   - `develop` - development branch
   - `feature/*` - feature branches
   - `fix/*` - bug fix branches

3. **Submodules:** Third-party C++ libraries are Git submodules
   - Never modify submodule code directly
   - Update submodules via `git submodule update --remote`

### Build & Release

1. **Product Flavors:**
   - `basic` - Standard release without NSFW filter
   - `filter` - Release with NSFW content filter

2. **Signing:**
   - Debug: Default debug keystore
   - Release: Configure via `gradle.properties`:
     - `RELEASE_STORE_FILE`
     - `RELEASE_STORE_PASSWORD`
     - `RELEASE_KEY_ALIAS`
     - `RELEASE_KEY_PASSWORD`

3. **APK Output:** Named `LocalDream_armv8a_{versionName}.apk`

### Performance Considerations

1. **NPU vs CPU/GPU:**
   - Prefer NPU when available (faster, lower power)
   - Fall back to CPU/GPU on unsupported devices
   - Check device capabilities before model loading

2. **Memory Management:**
   - Large tensor operations should be pooled
   - Use zstd compression for model storage
   - Implement proper cleanup in services

3. **Image Processing:**
   - Use stb_image for efficient image I/O
   - Implement tiling for large images
   - Cache processed images when possible

### Security

1. **Network:** Use HTTPS for model downloads
2. **Storage:** Validate file paths, avoid path traversal
3. **NDK:** Use hidden visibility for native symbols
4. **ProGuard:** Enable minification for release builds

### Testing

1. **Unit Tests:** Test utility functions and data models
2. **UI Tests:** Test critical user flows
3. **Instrumented Tests:** Test on real devices when possible
4. **Native Tests:** Test C++ functions independently

---

## Development Environment Setup

### Prerequisites

1. **Android Studio** (latest stable)
2. **Android SDK** (API 36+)
3. **Android NDK** r28
4. **CMake** 3.18+
5. **Qualcomm QNN SDK** 2.39.0 (for NPU development)
6. **Git** with submodule support

### Environment Variables

```bash
# Android NDK
ANDROID_NDK_ROOT=/path/to/android-ndk-r28

# QNN SDK (for native builds)
QNN_SDK_ROOT=/path/to/qairt/2.39.0.250926
```

### IDE Configuration

1. **EditorConfig:** Already configured in `.editorconfig`
2. **ktlint:** Integrated via Gradle plugin
3. **detekt:** Configured in `app/detekt.yml`
4. **CMake:** Use presets for native builds

---

## Common Tasks

### Adding a New Screen

1. Create composable in `ui/screens/YourScreen.kt`
2. Add route in `navigation/Navigation.kt`
3. Add navigation action if needed

### Adding a New Data Model

1. Create data class in `data/` directory
2. Add Room entity if persistent storage needed
3. Update database version and add migration

### Modifying Native Code

1. Edit files in `app/src/main/cpp/src/`
2. Run `build.bat` or `build.sh` to compile
3. Test on device with NPU support

### Updating Dependencies

1. Edit `gradle/libs.versions.toml`
2. Run `.\gradlew.bat dependencies` to verify
3. Test thoroughly for breaking changes

---

## Troubleshooting

### Build Issues

- **NDK not found:** Set `ANDROID_NDK_ROOT` environment variable
- **CMake errors:** Ensure CMake 3.18+ is installed
- **QNN SDK errors:** Verify QNN SDK path in `CMakeLists.txt`

### Runtime Issues

- **NPU not available:** Check device compatibility in README
- **Out of memory:** Reduce batch size or use smaller models
- **Slow inference:** Verify NPU acceleration is enabled

### Code Quality Failures

- **ktlint errors:** Run `.\gradlew.bat ktlintFormat`
- **detekt errors:** Review `app/detekt.yml` for suppressed rules

---

## Resources

- [Project README](README.md)
- [Guide Site](https://ld-guide.chino.icu)
- [Telegram Group](https://t.me/local_dream)
- [Qualcomm QNN SDK](https://www.qualcomm.com/developer/software/qualcomm-ai-engine-direct-sdk)
- [MNN Documentation](https://github.com/alibaba/MNN)

---

*Last updated: 2026-08-11*
