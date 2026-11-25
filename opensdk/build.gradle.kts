plugins {
    id("com.android.library")
}

android {
    namespace = "com.hack.opensdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        buildConfigField("String", "MASTER_PACKAGE", "\"com.atp.vm\"")
        buildConfigField("String", "ASSIST_PACKAGE", "\"com.atp.vm.assist\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
    }
}
