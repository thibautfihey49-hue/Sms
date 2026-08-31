plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.thibautfihey.sms"; compileSdk = 34
    defaultConfig { applicationId = "com.thibautfihey.sms"; minSdk = 26; targetSdk = 34; versionCode = 11; versionName = "6.0-ultra-light" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { compose = true }; composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
    buildTypes {
        release { isMinifyEnabled = true; isShrinkResources = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") }
        debug { isMinifyEnabled = false }
    }
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.navigation:navigation-compose:2.7.6")
}
