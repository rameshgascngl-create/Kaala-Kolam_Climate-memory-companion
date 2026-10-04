plugins {
    alias(libs.plugins.android.application)
}

// Release signing is optional. Values may come from ~/.gradle/gradle.properties or CI environment variables.
fun secret(name: String): String? = providers.gradleProperty(name).orNull
    ?: providers.environmentVariable(name).orNull

val ksFile = secret("KK_STORE_FILE")
val ksPassword = secret("KK_STORE_PASSWORD")
val keyAliasValue = secret("KK_KEY_ALIAS")
val keyPasswordValue = secret("KK_KEY_PASSWORD")
val hasReleaseSigning = listOf(ksFile, ksPassword, keyAliasValue, keyPasswordValue).all { !it.isNullOrBlank() }

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
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(ksFile!!)
                storePassword = ksPassword
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
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
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
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
