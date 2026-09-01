plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.jmwl.gostudio.shell"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jmwl.gostudio.shell"
        minSdk = 26
        // 28 以下允许执行应用私有目录中的文件（W^X 限制）
        targetSdk = 28
        versionCode = 1
        versionName = "1.0"
        ndk { abiFilters.add("arm64-v8a") }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            lint { checkReleaseBuilds = false }
        }
    }
    lint { abortOnError = false }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget("17"))
        }
    }
}

dependencies {
    implementation("com.google.android.material:material:1.12.0")
}
