plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.smartstick"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.smartstick"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("com.github.AtifSayings:Animatoo:1.0.1")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
//    implementation("com.google.android.libraries.places:places:4.2.0")
    implementation("com.google.android.libraries.places:places:3.4.0") {
        exclude(group = "com.google.android.gms", module = "play-services-maps")
    }
    // implementation("com.github.hannesa2:paho.mqtt.android:4.3") // Uncomment if needed

    // Remove play-services-maps to avoid duplicate classes
    // implementation("com.google.android.gms:play-services-maps:19.1.0")

    // Keep location services for FusedLocationProviderClient
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Optional: Maps utilities (if needed for polyline decoding, etc.)


    // Navigation SDK (includes map functionality)
    implementation("com.google.android.libraries.navigation:navigation:6.2.0")

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}