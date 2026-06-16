import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

version = "0.1.4"

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) load(file.inputStream())
}

fun versionCodeFromName(flavorSuffix: Int): Int {
    val regex = """^(\d+)\.(\d+)\.(\d+)$""".toRegex()
    val match = regex.matchEntire(version.toString())
        ?: error("version must be in M.m.p format, got: $version")
    val (major, minor, patch) = match.destructured
    return major.toInt() * 1_000_000 + minor.toInt() * 10_000 + patch.toInt() * 100 + flavorSuffix
}

android {
    namespace = "ru.fpvladder.laps.trainer"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "ru.fpvladder.laps.trainer"
        minSdk = 31
        targetSdk = 36
        versionName = version.toString()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(keystoreProperties.getProperty("keystore.path"))
            storePassword = keystoreProperties.getProperty("keystore.password")
            keyAlias = keystoreProperties.getProperty("key.alias")
            keyPassword = keystoreProperties.getProperty("key.password")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            signingConfig = signingConfigs.named("release").get()
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("rustore") {
            dimension = "distribution"
            versionCode = versionCodeFromName(0)
            buildConfigField("String", "VIP_BADGE_TEXT", "\"\"")
        }
        create("vip") {
            dimension = "distribution"
            isDefault = true
            versionCode = versionCodeFromName(1)
            val vipProperties = Properties().apply {
                val file = rootProject.file("vip.properties")
                if (file.exists()) load(file.inputStream())
            }
            val badgeText = vipProperties.getProperty("vip.badge.text").orEmpty()
            buildConfigField("String", "VIP_BADGE_TEXT", "\"$badgeText\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    applicationVariants.all {
        val variant = this
        outputs.configureEach {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val suffixPart = variant.buildType.applicationIdSuffix.orEmpty().replace(".", "-")
            output.outputFileName = "LAPS.Trainer-v${variant.versionName}-${variant.flavorName}${suffixPart}.apk"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kaml)
    implementation(libs.kotlinx.serialization.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
