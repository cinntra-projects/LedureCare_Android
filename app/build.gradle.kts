plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.ledurecare"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ledurecare"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // One switch per area: true = built-in sample data, false = the real API.
        // Flip each to false as that area's API is integrated.
        buildConfigField("boolean", "USE_STATIC_AUTH", "false")
        buildConfigField("boolean", "USE_STATIC_PASSWORD_RESET", "false")
        buildConfigField("boolean", "USE_STATIC_DEALERS", "false")  // Add dealers sheet, My Dealers, audit dealer list
        buildConfigField("boolean", "USE_STATIC_PLAN", "false")     // My Plan: list, add, edit
        buildConfigField("boolean", "USE_STATIC_WAREHOUSES", "false") // audit form warehouse dropdown
        buildConfigField("boolean", "USE_STATIC_AUDIT", "false")    // audit form submit (conditions stay sample)
        buildConfigField("boolean", "USE_STATIC_PRODUCTS", "false") // audit form item search
        buildConfigField("boolean", "USE_STATIC_STAGES", "false")   // Reports stage cards
        buildConfigField("boolean", "USE_STATIC_DLIS", "false")     // DLI list and detail
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"http://103.197.76.50:8107/\"")
            buildConfigField("String", "BP_BASE_URL", "\"http://103.107.67.160:8003/\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "BASE_URL", "\"https://api.example.com/\"")
            buildConfigField("String", "BP_BASE_URL", "\"https://bp.example.com/\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    // Shared APKs are named ledure_care_<versionName>[_<flavor>]_<buildType>.apk, e.g. ledure_care_1.0_debug.apk.
    applicationVariants.all {
        val variant = this
        outputs.all {
            val impl = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            impl.outputFileName = listOf("ledure_care", variant.versionName, variant.flavorName.takeIf { it.isNotEmpty() }, variant.buildType.name)
                .filterNotNull().joinToString("_") + ".apk"
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // AndroidX / Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.coil.compose)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Async
    implementation(libs.kotlinx.coroutines.android)

    // Local data
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.tink.android)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.timber)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
