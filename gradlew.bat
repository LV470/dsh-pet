@rem DSH 桌宠 · Gradle wrapper 启动器（精简版）
@rem 用法: gradlew.bat assembleDebug
@echo off
setlocal
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.\
java -classpath "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
endlocal
