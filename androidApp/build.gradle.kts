plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.tripmate.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tripmate.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        // TopAppBar/Scaffold usage in the screens below is still marked
        // @ExperimentalMaterial3Api upstream; opting in at the module level
        // avoids sprinkling @OptIn across every screen file.
        freeCompilerArgs += listOf("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }
    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.work.runtime)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.datetime)
    implementation("androidx.navigation:navigation-compose:2.8.1")
    implementation("io.insert-koin:koin-compose:1.1.5")

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.android)
    implementation(libs.firebase.firestore.android)
    implementation(libs.firebase.auth.android)
}
