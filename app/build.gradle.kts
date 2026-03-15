import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties().apply {
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.fourdigital.marketintelligence"
    compileSdk = rootProject.extra["compileSdk"] as Int

    defaultConfig {
        applicationId = "com.fourdigital.marketintelligence"
        minSdk = rootProject.extra["minSdk"] as Int
        targetSdk = rootProject.extra["targetSdk"] as Int
        versionCode = rootProject.extra["versionCode"] as Int
        versionName = rootProject.extra["versionName"] as String
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "APP_NAME", "\"4D Market Intelligence\"")
        buildConfigField("String", "DEVELOPER", "\"Daniel Dorsch\"")
        buildConfigField("String", "COMPANY", "\"4D Digital Solutions\"")
        buildConfigField("String", "DEFAULT_FINNHUB_KEY", "\"\"")
        buildConfigField("String", "DEFAULT_BRAPI_KEY", "\"\"")
        buildConfigField("String", "DEFAULT_GITHUB_KEY", "\"\"")
        buildConfigField("String", "DEFAULT_OPENAI_KEY", "\"\"")
        buildConfigField("String", "DEFAULT_TWELVEDATA_KEY", "\"\"")
        buildConfigField("String", "DEFAULT_MASSIVE_KEY", "\"\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("${rootProject.projectDir}/${keystoreProperties.getProperty("storeFile", "release-keystore.jks")}")
            storePassword = keystoreProperties.getProperty("storePassword", "")
            keyAlias = keystoreProperties.getProperty("keyAlias", "")
            keyPassword = keystoreProperties.getProperty("keyPassword", "")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            buildConfigField("Boolean", "DEMO_MODE", "true")
            buildConfigField("String", "DEFAULT_FINNHUB_KEY", "\"${localProperties.getProperty("DEFAULT_FINNHUB_KEY", "")}\"")
            buildConfigField("String", "DEFAULT_BRAPI_KEY", "\"${localProperties.getProperty("DEFAULT_BRAPI_KEY", "")}\"")
            buildConfigField("String", "DEFAULT_GITHUB_KEY", "\"${localProperties.getProperty("DEFAULT_GITHUB_KEY", "")}\"")
            buildConfigField("String", "DEFAULT_OPENAI_KEY", "\"${localProperties.getProperty("DEFAULT_OPENAI_KEY", "")}\"")
            buildConfigField("String", "DEFAULT_TWELVEDATA_KEY", "\"${localProperties.getProperty("DEFAULT_TWELVEDATA_KEY", "")}\"")
            buildConfigField("String", "DEFAULT_MASSIVE_KEY", "\"${localProperties.getProperty("DEFAULT_MASSIVE_KEY", "")}\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("Boolean", "DEMO_MODE", "false")
            buildConfigField("String", "DEFAULT_FINNHUB_KEY", "\"${localProperties.getProperty("DEFAULT_FINNHUB_KEY", "")}\"")
            buildConfigField("String", "DEFAULT_BRAPI_KEY", "\"${localProperties.getProperty("DEFAULT_BRAPI_KEY", "")}\"")
            buildConfigField("String", "DEFAULT_GITHUB_KEY", "\"${localProperties.getProperty("DEFAULT_GITHUB_KEY", "")}\"")
            buildConfigField("String", "DEFAULT_OPENAI_KEY", "\"${localProperties.getProperty("DEFAULT_OPENAI_KEY", "")}\"")
            buildConfigField("String", "DEFAULT_TWELVEDATA_KEY", "\"${localProperties.getProperty("DEFAULT_TWELVEDATA_KEY", "")}\"")
            buildConfigField("String", "DEFAULT_MASSIVE_KEY", "\"${localProperties.getProperty("DEFAULT_MASSIVE_KEY", "")}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core-ui"))
    implementation(project(":core-network"))
    implementation(project(":core-database"))
    implementation(project(":core-common"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":analytics"))
    implementation(project(":feature-watchlist"))
    implementation(project(":feature-market-overview"))
    implementation(project(":feature-correlations"))
    implementation(project(":feature-signals"))
    implementation(project(":feature-alerts"))
    implementation(project(":feature-world-clock"))
    implementation(project(":feature-settings"))
    implementation(project(":feature-github-sync"))

    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.work.runtime)
    implementation(libs.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(libs.test.runner)
}
