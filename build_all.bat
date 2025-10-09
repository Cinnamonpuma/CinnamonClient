@echo off
setlocal enabledelayedexpansion

:: ==========================================
:: Multi-version Fabric build script
:: ==========================================

:: === Versions to build ===
set VERSIONS=1.21.6 1.21.7 1.21.8

echo.
echo ===============================
echo 🔧 Starting multi-version build
echo ===============================

:: === Backup current gradle.properties ===
if exist gradle.properties.backup del gradle.properties.backup >nul
copy /Y gradle.properties gradle.properties.backup >nul
echo Backed up gradle.properties

:: === Extract the top (static) section of gradle.properties ===
set "found="
>gradle_top.tmp (
    for /f "delims=" %%A in (gradle.properties) do (
        echo %%A | findstr /b "minecraft_version=" >nul && set found=1
        if not defined found echo %%A
    )
)

:: === Build for each version ===
for %%v in (%VERSIONS%) do (
    echo.
    echo ===============================
    echo 🧱 Building for Minecraft %%v...
    echo ===============================

    if exist versions\%%v.properties (
        (type gradle_top.tmp) > gradle.properties
        type versions\%%v.properties >> gradle.properties
        echo. >> gradle.properties
        echo # Mod Properties >> gradle.properties
        findstr /B "mod_version= maven_group= archives_base_name=" gradle.properties.backup >> gradle.properties
        echo. >> gradle.properties
        echo # Dependencies >> gradle.properties

        :: Only clean on the first build to save time
        if not defined cleaned (
            echo Cleaning project...
            call gradlew clean >nul
            set cleaned=1
        )

        echo Building...
        call gradlew build -x remapJar

        :: Move built jars into versioned subfolder
        if exist build\libs (
            mkdir build\libs\%%v 2>nul
            for %%f in (build\libs\*.jar) do (
                if not "%%~nxf"=="gradle-wrapper.jar" (
                    move /Y "%%f" "build\libs\%%v\%%~nf-mc%%v%%~xf" >nul
                )
            )
        )

        echo ✅ Finished building for %%v
    ) else (
        echo ❌ ERROR: Missing versions\%%v.properties!
    )
)

:: === Restore the original gradle.properties ===
copy /Y gradle.properties.backup gradle.properties >nul

:: === Clean up temporary files ===
del gradle_top.tmp >nul 2>nul
del gradle.properties.backup >nul 2>nul

echo.
echo ===============================
echo ✅ All builds complete!
echo Outputs: build/libs/<version>/
echo ===============================
pause
