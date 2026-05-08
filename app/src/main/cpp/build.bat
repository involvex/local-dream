@echo off
setlocal EnableDelayedExpansion

REM Set default Android NDK path if not set
if "%ANDROID_NDK_ROOT%"=="" (
    set "ANDROID_NDK_ROOT=C:\Users\lukas\AppData\Local\Android\Sdk\ndk\28.2.13676358"
)

REM Set default QNN SDK path if not set
if "%QNN_SDK_ROOT%"=="" (
    set "QNN_SDK_ROOT=D:\repos\local-dream\app\src\main\cpp\qairt\2.39.0.250926"
)

REM Convert patch line endings (required on Windows)
dos2unix SampleApp.patch 2>nul

cmake --preset android-release
if %ERRORLEVEL% neq 0 goto :error

cmake --build --preset android-release
if %ERRORLEVEL% neq 0 goto :error

if not exist lib mkdir lib
xcopy /Y /E .\build\android\qnnlibs ..\assets\qnnlibs\
if %ERRORLEVEL% neq 0 goto :error

if not exist ..\jniLibs\arm64-v8a mkdir ..\jniLibs\arm64-v8a
xcopy /Y .\build\android\bin\arm64-v8a\libstable_diffusion_core.so ..\jniLibs\arm64-v8a\
if %ERRORLEVEL% neq 0 goto :error

echo Build completed successfully
goto :eof

:error
echo Failed with error #%ERRORLEVEL%.
exit /b %ERRORLEVEL%
