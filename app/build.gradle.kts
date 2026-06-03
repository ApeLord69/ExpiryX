// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kapt)
    alias(libs.plugins.parcelize)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.expiryx.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.expiryx.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0-alpha" // version shown in Settings

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // FIX: wrap in provider so Gradle evaluates properly
        resValue("string", "app_version_name", versionName ?: "unknown")
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Enable 16KB page alignment
            // https://developer.android.com/guide/practices/page-sizes#check-alignment
            packaging {
                jniLibs {
                    useLegacyPackaging = false
                }
            }
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
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}



dependencies {
    // AndroidX core UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.firebase.database)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.fragment:fragment-ktx:1.8.4")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    // Room
    implementation(libs.room.runtime)
    kapt(libs.room.compiler)
    implementation(libs.room.ktx)

    // Google Sign-In & Firebase
    implementation(libs.play.services.auth)
    implementation("com.google.android.gms:play-services-base:18.10.0")
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // CameraX
    implementation("androidx.camera:camera-core:1.4.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")

    // ML Kit
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("org.json:json:20230227")

    // Guava (Fix for ListenableFuture issue)
    implementation(libs.guava)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Apache POI (Excel export)


    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
