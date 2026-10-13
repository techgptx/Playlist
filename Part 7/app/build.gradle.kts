plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

fun configuredValue(name: String): String? =
    providers.gradleProperty(name).orNull ?: providers.environmentVariable(name).orNull

fun buildConfigString(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val releaseStorePath = configuredValue("JARVIS_RELEASE_STORE_FILE")
val releaseStorePassword = configuredValue("JARVIS_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = configuredValue("JARVIS_RELEASE_KEY_ALIAS")
val releaseKeyPassword = configuredValue("JARVIS_RELEASE_KEY_PASSWORD")
val releaseCertSha256 = configuredValue("JARVIS_RELEASE_CERT_SHA256")?.replace(":", "")?.uppercase().orEmpty()
val developerName = configuredValue("JARVIS_DEVELOPER_NAME")?.ifBlank { null } ?: "Satyam sahani"
val copyrightOwner = configuredValue("JARVIS_COPYRIGHT_OWNER")?.ifBlank { null } ?: "Satyam sahani"
val verificationEndpoint = configuredValue("JARVIS_VERIFICATION_ENDPOINT").orEmpty()
val releaseSigningReady = listOf(releaseStorePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { !it.isNullOrBlank() }

android {
    namespace = "com.jarvis.assistant"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jarvis.assistant"
        minSdk = 26
        targetSdk = 34
        versionCode = 143
        versionName = "1.8.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "DEVELOPER_NAME", buildConfigString(developerName.ifBlank { "Unverified developer" }))
        buildConfigField("String", "COPYRIGHT_OWNER", buildConfigString(copyrightOwner.ifBlank { "Unconfigured" }))
        buildConfigField("String", "PINNED_RELEASE_CERT_SHA256", buildConfigString(releaseCertSha256))
        buildConfigField("String", "VERIFICATION_ENDPOINT", buildConfigString(verificationEndpoint))
    }

    signingConfigs {
        create("release") {
            if (releaseSigningReady) {
                storeFile = file(releaseStorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    lint {
        // AndroidX Lifecycle 2.8.6's NullSafeMutableLiveData detector crashes under this project's Kotlin 2.0 UAST.
        // Keep all other lint checks enabled while that upstream detector is incompatible.
        disable += "NullSafeMutableLiveData"
    }
}

tasks.register("verifyReleaseSecurityInputs") {
    doLast {
        check(releaseSigningReady) {
            "Release signing is not configured. Set JARVIS_RELEASE_STORE_FILE, JARVIS_RELEASE_STORE_PASSWORD, JARVIS_RELEASE_KEY_ALIAS, and JARVIS_RELEASE_KEY_PASSWORD in the local environment or Gradle user properties."
        }
        check(releaseCertSha256.matches(Regex("[A-F0-9]{64}"))) {
            "Set JARVIS_RELEASE_CERT_SHA256 to the SHA-256 fingerprint of the release signing certificate."
        }
        check(developerName.isNotBlank() && copyrightOwner.isNotBlank()) {
            "Set JARVIS_DEVELOPER_NAME and JARVIS_COPYRIGHT_OWNER to the verified publisher identity."
        }
    }
}

tasks.matching { it.name == "packageRelease" }.configureEach {
    dependsOn("verifyReleaseSecurityInputs")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.webkit:webkit:1.12.0")
    implementation("net.java.dev.jna:jna:5.13.0@aar")
    implementation("com.alphacephei:vosk-android:0.3.75@aar")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Security Crypto for EncryptedSharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    testImplementation("junit:junit:4.13.2")
}


