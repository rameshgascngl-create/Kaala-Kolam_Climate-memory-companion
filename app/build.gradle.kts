plugins {
    alias(libs.plugins.android.application)
}

// Release signing is read from ~/.gradle/gradle.properties; no secret belongs in this repository.
val ksFile = providers.gradleProperty("KK_STORE_FILE").orNull

android {
    namespace = "edu.gascnagercoil.kaalakolam"
    compileSdk = 36

    defaultConfig {
        applicationId = "edu.gascnagercoil.kaalakolam"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (ksFile != null) {
            create("release") {
                storeFile = file(ksFile)
                storePassword = providers.gradleProperty("KK_STORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("KK_KEY_ALIAS").get()
                keyPassword = providers.gradleProperty("KK_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (ksFile != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.webkit)
}
