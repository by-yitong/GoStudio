plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt")
}

android {
    namespace = "com.xc.code"
    ndkVersion = "29.0.14206865"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.xc.code"
        minSdk = 26
        targetSdk = 28
        versionCode = 100
        versionName = "1.0.0"
        
        ndk {
            abiFilters.add("arm64-v8a")
        }
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    
    lint {
        disable.add("ExpiredTargetSdkVersion")
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    signingConfigs {
        create("release") {
            keyAlias = "release"
            keyPassword = "11754840"
            storePassword = "11754840"
            storeFile = file("xcode-release-key.jks")
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget("17"))
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.apache.commons.compress)
    implementation(libs.xz)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    
    implementation(libs.permissions.dispatcher)
    kapt(libs.permissions.dispatcher.processor)
    
    implementation(project(":modules:terminal-view"))
    implementation(project(":modules:editor-core"))
    implementation(project(":modules:project-file-tree"))
    implementation(project(":modules:toolchain-runtime"))
    implementation(project(":modules:clangd-lsp"))
    implementation(project(":modules:sora-editor"))
    implementation(project(":modules:sora-language-textmate"))
    implementation(project(":modules:sora-oniguruma-native"))
    
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("io.getstream:stream-chat-android-compose:7.3.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}