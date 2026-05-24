plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.kapt")
}

android {
  namespace = "com.cubelens"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.cubelens"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "0.1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables {
      useSupportLibrary = true
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  kotlinOptions {
    jvmTarget = "21"
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = "1.5.14"
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
  lint {
    disable += setOf("Instantiatable", "PermissionImpliesUnsupportedChromeOsHardware", "LockedOrientationActivity", "DiscouragedApi", "GradleDependency")
  }
}

dependencies {
  val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
  // composeBom 2024.06.00 has accessibility crash on ColorOS 16 — upgrade to latest
  @Suppress("DEPRECATION")
  implementation(composeBom)
  androidTestImplementation(composeBom)

  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
  implementation("androidx.activity:activity-compose:1.9.1")

  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.ui:ui-tooling-preview")
  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
  implementation("androidx.compose.material3:material3")

  implementation("androidx.navigation:navigation-compose:2.7.7")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

  // CameraX
  val cameraXVersion = "1.3.4"
  implementation("androidx.camera:camera-core:$cameraXVersion")
  implementation("androidx.camera:camera-camera2:$cameraXVersion")
  implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
  implementation("androidx.camera:camera-view:$cameraXVersion")
  implementation("androidx.exifinterface:exifinterface:1.3.7")

  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

  // Room
  implementation("androidx.room:room-runtime:2.6.1")
  implementation("androidx.room:room-ktx:2.6.1")
  annotationProcessor("androidx.room:room-compiler:2.6.1")
  //noinspection KaptUsageInsteadOfKsp
  kapt("androidx.room:room-compiler:2.6.1")

  // DataStore for preferences
  implementation("androidx.datastore:datastore-preferences:1.1.1")

  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.2.1")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
  androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
