# DSH 桌宠 · 一键编译（Windows PowerShell）
# 用法：在项目根目录执行  powershell -ExecutionPolicy Bypass -File build.ps1
$ErrorActionPreference = "Continue"
function Say($t, $c) { Write-Host $t -ForegroundColor $c }

Say "==========================================" Cyan
Say "   DSH 桌宠 · 一键编译" Cyan
Say "==========================================" Cyan

# ---------- 1. JDK ----------
Say "`n[1/4] 检查 JDK ..." Yellow
$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
    Say "  X 没找到 java" Red
    Say "  请先执行：  winget install Microsoft.OpenJDK.17" White
    exit 1
}
$v = (& java -version 2>&1 | Select-Object -First 1)
Say "  OK $v" Green

# ---------- 2. Android SDK ----------
Say "`n[2/4] 检查 Android SDK ..." Yellow
if (-not $env:ANDROID_HOME) {
    foreach ($p in @("$env:LOCALAPPDATA\Android\Sdk", "C:\android-sdk", "$env:USERPROFILE\android-sdk", "$env:USERPROFILE\AppData\Local\Android\Sdk")) {
        if (Test-Path (Join-Path $p "platforms")) { $env:ANDROID_HOME = $p; break }
    }
}
if (-not $env:ANDROID_HOME) {
    Say "  X 没找到 Android SDK" Red
    Say "  请先按 README「方案 B」第 1~2 步装命令行工具，并设好 ANDROID_HOME" White
    exit 1
}
Say "  OK ANDROID_HOME = $env:ANDROID_HOME" Green

$plat = Join-Path $env:ANDROID_HOME "platforms\android-34"
if (-not (Test-Path $plat)) { Say "  ! 缺少 platforms;android-34，正在补装 ..." Yellow
    & (Join-Path $env:ANDROID_HOME "cmdline-tools\latest\bin\sdkmanager.bat") "platforms;android-34" "build-tools;34.0.0" "platform-tools" }

# ---------- 3. local.properties ----------
Say "`n[3/4] 写 local.properties ..." Yellow
$sdkEsc = $env:ANDROID_HOME -replace '\\', '\\\\'
[System.IO.File]::WriteAllText((Join-Path $PWD "local.properties"), "sdk.dir=$sdkEsc`n")
Say "  OK sdk.dir=$sdkEsc" Green

# ---------- 4. 编译 ----------
Say "`n[4/4] 开始编译（首次要下 Gradle 和依赖，约 5~15 分钟）..." Yellow
if (Test-Path ".\gradlew.bat") { & .\gradlew.bat assembleDebug --no-daemon }
else { gradle assembleDebug --no-daemon }

if ($LASTEXITCODE -ne 0) {
    Say "`n  X 编译失败 —— 把上面红色/出错部分截图发我" Red
    exit 1
}

$apk = Get-ChildItem -Recurse -Filter "*.apk" "app\build\outputs\apk\debug\" -ErrorAction SilentlyContinue | Select-Object -First 1
Say "`n==========================================" Green
Say "   编译成功！" Green
if ($apk) {
    Say "   APK : $($apk.FullName)" Green
    Say "   体积: $([math]::Round($apk.Length/1MB,2)) MB" Green
}
Say "==========================================" Green
