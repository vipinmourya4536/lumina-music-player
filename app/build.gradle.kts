plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.lumina.music"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lumina.music"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
    }

    // Release signing is configurable and optional. Provide it either via
    // environment variables (LUMINA_KEYSTORE, LUMINA_KEYSTORE_PASSWORD,
    // LUMINA_KEY_ALIAS, LUMINA_KEY_PASSWORD) or via Gradle properties of the
    // same names in ~/.gradle/gradle.properties. When no keystore is found the
    // release build simply stays unsigned, so anyone can build the project.
    val luminaProp: (String) -> String? = { name ->
        System.getenv(name) ?: project.findProperty(name)?.toString()
    }
    val keystoreFile = luminaProp("LUMINA_KEYSTORE")?.let { rootProject.file(it) }
        ?: rootProject.file("keystore/lumina-release.jks")
    val keystoreAvailable = keystoreFile.exists()

    signingConfigs {
        if (keystoreAvailable) {
            create("release") {
                storeFile = keystoreFile
                storePassword = luminaProp("LUMINA_KEYSTORE_PASSWORD")
                keyAlias = luminaProp("LUMINA_KEY_ALIAS")
                keyPassword = luminaProp("LUMINA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            if (keystoreAvailable) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.navigation:navigation-compose:2.8.2")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.palette:palette-ktx:1.0.0")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.9.0")
    implementation("com.google.guava:guava:33.3.1-android")
}
