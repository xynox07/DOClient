plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.doclient.launcher"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.doclient.launcher"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "0.8.0"
    }
}
