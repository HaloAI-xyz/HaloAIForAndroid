plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.devtools.ksp")
}

android {
    namespace = "xyz.haloai.haloai_android_productivity"
    compileSdk = 34

    defaultConfig {
        applicationId = "xyz.haloai.haloai_android_productivity"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    // For immutable list
    implementation(libs.guava)
    implementation(libs.androidx.constraintlayout.compose.android)
    // For requesting permissions with compose (better UI)
    implementation(libs.accompanist.permissions)
    // For Room database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Koin core features
    implementation(libs.koin.core)
    // Koin Android features
    implementation(libs.koin.android)
    // Koin AndroidX ViewModel features
    // implementation(libs.koin.androidx.viewmodel)
    // Koin Compose
    implementation(libs.koin.androidx.compose)

    // Gmail API
    implementation(libs.google.api.services.gmail)
    // Google Auth
    implementation (libs.google.oauth.client.jetty)
    implementation (libs.google.auth.library.oauth2.http)
    implementation (libs.google.api.client.android)
    implementation (libs.google.http.client.android)
    // Google Calendar API
    implementation (libs.google.api.services.calendar)
    implementation(libs.play.services.auth)
    // OpenAI API
    implementation(libs.openai.client)
    implementation(libs.ktor.client.android)
    // Azure KeyVault
    implementation(libs.azure.security.keyvault.secrets)
    implementation(libs.azure.identity)
    // .env file loading
    implementation(libs.dotenv.kotlin)
    // Microsoft Graph API
    implementation(libs.msal)
    implementation(libs.volley)
    implementation(libs.microsoft.graph)
    // Json parsing
    implementation(libs.ksoup)
    // Glance for Android
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material)
    implementation(libs.androidx.runtime.livedata)
    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    // MLKit
    implementation(libs.text.recognition)
}