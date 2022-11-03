plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")

    // TODO: androidTest not starting if this is uncommented, WHY???
    id("com.google.gms.google-services")

    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    compileSdk = Version.COMPILE_SDK

    defaultConfig {
        minSdk = Version.MIN_SDK
        targetSdk = Version.TARGET_SDK

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    signingConfigs {
        create("release") {
            storeFile = file("D:\\dev\\Android\\Android Keys\\profile_release_key.keystore")
            storePassword = "profile_release_key"
            keyAlias = "profile_release_key"
            keyPassword = "profile_release_key"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    namespace = "com.tachyonmusic.user"
}

dependencies {
    firebase()
    firebaseAnalytics()

    coroutines()
    lifecycle()
    gson()
    dagger()

    projectCore()
    projectUtil()


    localTest()
    androidTest()
}