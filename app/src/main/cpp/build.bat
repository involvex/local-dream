@echo off
setlocal EnableDelayedExpansion

rem Locate an NDK: prefer ANDROID_NDK_ROOT, else the newest under the
rem Android SDK's ndk folder (sdkmanager-managed).
if "%ANDROID_NDK_ROOT%"=="" (
    for /f "delims=" %%i in ('dir /b /ad /o-n "%LOCALAPPDATA%\Android\Sdk\ndk" 2^>nul') do (
        if "!ANDROID_NDK_ROOT!"=="" set "ANDROID_NDK_ROOT=%LOCALAPPDATA%\Android\Sdk\ndk\%%i"
    )
)
if "%ANDROID_NDK_ROOT%"=="" (
    echo No NDK found. Set ANDROID_NDK_ROOT or install one via sdkmanager.
    exit /b 1
)
echo Using NDK: %ANDROID_NDK_ROOT%

set QNN_ARGS=
if not "%QNN_SDK_ROOT%"=="" set "QNN_ARGS=-DQNN_SDK_ROOT=%QNN_SDK_ROOT%"

cmake --preset android-release -DCMAKE_POLICY_VERSION_MINIMUM=3.5 %QNN_ARGS%
if %ERRORLEVEL% neq 0 goto :error

cmake --build --preset android-release
if %ERRORLEVEL% neq 0 goto :error

if not exist lib mkdir lib
xcopy /Y /E .\build\android\qnnlibs ..\assets\qnnlibs\
if %ERRORLEVEL% neq 0 goto :error

if not exist ..\jniLibs\arm64-v8a mkdir ..\jniLibs\arm64-v8a
xcopy /Y .\build\android\bin\arm64-v8a\libstable_diffusion_core.so ..\jniLibs\arm64-v8a\
if %ERRORLEVEL% neq 0 goto :error

rem LLM JNI library for the chat feature. Built from cpp/llm explicitly:
rem CMake presets cannot retarget the source directory, so a preset would
rem wrongly configure the root project.
pushd llm
cmake -B build -G Ninja -DCMAKE_BUILD_TYPE=Release -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-21 -DCMAKE_TOOLCHAIN_FILE="%ANDROID_NDK_ROOT%\build\cmake\android.toolchain.cmake" -DCMAKE_POLICY_VERSION_MINIMUM=3.5
if %ERRORLEVEL% neq 0 goto :error

cmake --build build
if %ERRORLEVEL% neq 0 goto :error
popd

xcopy /Y .\llm\build\obj\local\arm64-v8a\liblocaldream_llm.so ..\jniLibs\arm64-v8a\
if %ERRORLEVEL% neq 0 goto :error

echo Build completed successfully
goto :eof

:error
echo Failed with error #%ERRORLEVEL%.
exit /b %ERRORLEVEL%
