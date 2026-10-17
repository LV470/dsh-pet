plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.dsh.pet"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dsh.pet"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        // 只排掉真正会撞车的元数据，别用 META-INF/* 一刀切
        // （那会连 kotlin_module、依赖的服务文件一起删掉）
        resources.excludes += setOf(
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE",
            "META-INF/LICENSE.txt",
            "META-INF/NOTICE",
            "META-INF/NOTICE.txt",
            "META-INF/*.kotlin_module"
        )
    }
}

dependencies {
    // 只用 appcompat：网络走 HttpURLConnection，异步走 Handler，
    // 少一个依赖就少一分首次构建失败的可能
    implementation("androidx.appcompat:appcompat:1.7.0")
    // WebViewAssetLoader：把 file:// 资源映射成 https:// 域名，
    // 否则 pixi-live2d-display 内部用 fetch 加载模型会静默失败
    implementation("androidx.webkit:webkit:1.11.0")
}
