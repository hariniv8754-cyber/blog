@echo off
set "JAVA_HOME=C:\Users\jashwanth ck\.gemini\antigravity\scratch\jdk17\jdk-17.0.11+9"
set "PATH=%JAVA_HOME%\bin;C:\Users\jashwanth ck\.gemini\antigravity\scratch\gradle\gradle-8.7\bin;%PATH%"

echo Checking Java Version...
java -version

echo Checking Gradle Version...
call gradle.bat -version

echo Building Android APK...
cd /d "C:\Users\jashwanth ck\.gemini\antigravity\scratch\map-location-finder\android-tracker-app"
call gradle.bat assembleDebug --stacktrace --no-daemon

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    copy "app\build\outputs\apk\debug\app-debug.apk" "..\QuantumGPS-Tracker.apk" /Y
    echo ===================================================
    echo BUILD SUCCESS! APK created at:
    echo C:\Users\jashwanth ck\.gemini\antigravity\scratch\map-location-finder\QuantumGPS-Tracker.apk
    echo ===================================================
) else (
    echo ===================================================
    echo BUILD FAILED! Check error output above.
    echo ===================================================
)
