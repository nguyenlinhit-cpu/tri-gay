plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "local.vn.suppermario"
    buildToolsVersion = "37.0.0"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "local.vn.suppermario"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            // Local exports are installable; publishing builds keep their own signing key.
            if (providers.gradleProperty("localApk").orNull == "true") {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
