import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Secrets from local.properties (never committed) or environment; empty
// defaults keep CI green. Only sdk.dir is auto-loaded — project keys are
// read explicitly.
val localSecrets = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

fun secret(propName: String, envName: String, default: String): String =
    localSecrets.getProperty(propName)
        ?.takeIf { it.isNotBlank() }
        ?: System.getenv(envName)?.takeIf { it.isNotBlank() }
        ?: default

android {
    namespace = "ca.gastimate.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "ca.gastimate.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"${secret("api.baseUrl", "GASTIMATE_API_URL", "http://10.0.2.2:8000")}\"",
        )
        // App-embedded key (same trust model as MLCAndroid's Discord client id:
        // anything shipped in an APK is extractable; the backend revokes by
        // name when needed).
        buildConfigField(
            "String",
            "API_KEY",
            "\"${secret("api.key", "GASTIMATE_API_KEY", "")}\"",
        )
    }

    buildTypes {
        release {
            // Debug-signed for sideload testing only. Store releases must
            // wire a real keystore here (never commit it).
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.04.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    // Theme preference persistence (tiny single-pref store).
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // lintVitalRelease requires Fragment 1.3+ for the activity-result API;
    // a direct (not transitive) declaration satisfies the check.
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    // Station brand logos from GasBuddy's CDN.
    implementation("io.coil-kt:coil-compose:2.6.0")
    // Location button: balanced-power fixes + lastLocation fast path.
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
