plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.bypassapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bypassapp"
        minSdk = 31
        targetSdk = 35
        versionCode = 15
        versionName = "latest"
    }

    buildTypes {
        release {
            // TURNED OFF: This prevents R8 from deleting your Xposed hooks
            isMinifyEnabled = false 
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        // Upgraded to Java 17 for modern AGP support
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Xposed MUST be compileOnly so it doesn't bundle into the APK
    compileOnly(libs.xposed.api) 
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}
