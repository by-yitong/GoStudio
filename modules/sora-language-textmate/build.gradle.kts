plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.github.rosemoe.sora.langs.textmate"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

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
    compileOnly(project(":modules:sora-editor"))
    compileOnly(project(":modules:sora-oniguruma-native"))

    implementation("com.google.code.gson:gson:2.13.2")
    implementation("org.jruby.jcodings:jcodings:1.0.64")
    implementation("org.jruby.joni:joni:2.2.7")
    implementation("org.snakeyaml:snakeyaml-engine:3.0.1")
    implementation("org.eclipse.jdt:org.eclipse.jdt.annotation:2.4.100")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
