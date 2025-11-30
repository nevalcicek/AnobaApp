plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.kotlinx.serialization)
    id("org.jetbrains.kotlin.kapt")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    namespace = "com.neval.anoba"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.neval.anoba"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "2.0.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.ripple)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.animation.core)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.runtime.saveable)
    implementation(libs.androidx.ui.geometry)
    implementation(libs.androidx.ui.unit)
    implementation(libs.androidx.ui.text)
    implementation(libs.androidx.ui.util)
    implementation(libs.androidx.ui.tooling.data)
    implementation(libs.ui)
    implementation(libs.material3)
    implementation(libs.androidx.core.splashscreen)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.ktx)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.core)
    implementation(libs.koin.androidx.navigation)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // 🔧 Duplicate Classes hatasını çözmek için support-v4 exclude edildi
    implementation(libs.play.services.auth) {
        exclude(group = "com.android.support", module = "support-v4")
    }

    implementation(libs.androidx.compose.animation.graphics)
    implementation(libs.androidx.room.ktx)
    implementation(libs.generativeai)
    implementation(libs.androidx.tools.core)
    implementation("io.coil-kt:coil-video:2.6.0")
    implementation(libs.foundation.layout)

    kapt(libs.androidxRoomCompiler)

    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.room.runtime)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    testImplementation(libs.junit)
    debugImplementation(libs.ui.tooling)
}

kapt {
    correctErrorTypes = true
}