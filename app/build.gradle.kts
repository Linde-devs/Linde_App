plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "josue.com.bo.linde"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "josue.com.bo.linde"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // URL base de la API en un solo lugar.
        // Emulador: http://10.0.2.2:5080/api/  ·  Celular real: http://<IP-de-la-PC>:5080/api/ (mismo Wi-Fi).
        buildConfigField("String", "API_URL", "\"http://10.0.2.2:5080/api/\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation(libs.swiperefreshlayout)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.osmdroid)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
