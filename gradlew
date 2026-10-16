#!/bin/sh
# DSH 桌宠 · Gradle wrapper 启动器（精简版）
# 用法: ./gradlew assembleDebug
DIR="$(cd "$(dirname "$0")" && pwd)"
exec java -classpath "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
