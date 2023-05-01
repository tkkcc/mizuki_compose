@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.bilabila.mizuki"
//    buildToolsVersion = "33.0.2"
//    ndkVersion = "25.1.8937393"
    compileSdk = 33
    defaultConfig {
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }
    signingConfigs {
        create("release") {
            val keystoreProperties = gradleLocalProperties(rootDir)
            keyAlias = keystoreProperties.getOrDefault("keyAlias", "") as String
            keyPassword = keystoreProperties.getOrDefault("keyPassword", "") as String
            storeFile = file(keystoreProperties.getOrDefault("storeFile", ".") as String)
            storePassword = keystoreProperties.getOrDefault("storePassword", "") as String
        }
        named("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildTypes {
        named("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    flavorDimensions.add("pipeline")
    productFlavors {
        create("full") {
            dimension = "pipeline"
        }
        create("user") {
            dimension = "pipeline"
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

val fullImplementation by configurations
val userImplementation by configurations

dependencies {
    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    fullImplementation(project(":ui"))
}
