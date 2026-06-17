import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlin.android)
    id("org.lsposed.lsplugin.jgit") version "1.1" 
    id("org.lsposed.lsplugin.resopt") version "1.6" 
    id("org.lsposed.lsplugin.apksign") version "1.4"
    id("org.lsposed.lsplugin.apktransform") version "1.2" 
}

fun gitOutput(vararg args: String): String? {
    return runCatching {
        providers.exec {
            commandLine("git", *args)
        }
            .standardOutput
            .asText
            .get()
            .trim()
            .takeIf { it.isNotEmpty() }
    }.getOrNull()
}

val releaseCode = gitOutput("rev-list", "--count", "HEAD")?.toIntOrNull() ?: 1
val releaseName: String by rootProject

apksign {
    storeFileProperty = "releaseStoreFile"
    storePasswordProperty = "releaseStorePassword"
    keyAliasProperty = "releaseKeyAlias"
    keyPasswordProperty = "releaseKeyPassword"
}

apktransform {
    copy {
        when (it.buildType) {
            "release" -> file("${it.name}/bbzq_v${releaseName}-${releaseCode}.apk")
            else -> null
        }
    }
}

android {
    namespace = "io.github.bbzq"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.bbzq"
        minSdk = 24
        targetSdk = 35
        versionCode = releaseCode
        versionName = "v${releaseName}-${releaseCode}"

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions",
            "-language-version=2.0",
            )
        }
    }
}

configurations.all {
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk7")
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
}

dependencies {
    compileOnly(libs.libxposed.api)
    compileOnly(libs.xposed.api)
    implementation(libs.libxposed.service)
    implementation(libs.okhttp)
}
