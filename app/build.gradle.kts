import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// --- AdMob : identifiants de TEST officiels Google (sans risque) ------------
val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val testAdmobBannerId = "ca-app-pub-3940256099942544/9214589741"

// Identifiants réels : lus depuis ~/.gradle/gradle.properties (hors dépôt).
val releaseAdmobAppId = (project.findProperty("medfind.admob.appId") as String?) ?: testAdmobAppId
val releaseAdmobBannerId = (project.findProperty("medfind.admob.bannerId") as String?) ?: testAdmobBannerId
val adsEnabled = (project.findProperty("medfind.ads.enabled") as String?)?.toBoolean() ?: true

android {
    namespace = "com.medfind.maroc"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.medfind.maroc"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("boolean", "ADS_ENABLED", adsEnabled.toString())
        buildConfigField("String", "ADMOB_BANNER_ID", "\"$testAdmobBannerId\"")
        manifestPlaceholders["admobAppId"] = testAdmobAppId
    }

    buildTypes {
        debug {
            // Debug : toujours les identifiants de test.
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$testAdmobBannerId\"")
            manifestPlaceholders["admobAppId"] = testAdmobAppId
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$releaseAdmobBannerId\"")
            manifestPlaceholders["admobAppId"] = releaseAdmobAppId
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.osmdroid.android)
    implementation(libs.play.services.ads)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
