plugins {
    kotlin("android")
    id("com.android.application")
}

android {
    compileSdk = 34
    buildToolsVersion = "34.0.0"

    defaultConfig {
        minSdk = 25
        targetSdk = 34
        versionCode = 200
        versionName = "20.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    namespace = "org.owoTrack.Wear"
}

dependencies {
    implementation(project(":owoTrackLib"))
    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    implementation("androidx.percentlayout:percentlayout:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.wear:wear:1.3.0")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("androidx.preference:preference:1.2.1")

}