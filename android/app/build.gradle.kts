plugins {
    id("com.android.application")
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.example.pinpad_app"
    compileSdk = 34
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    defaultConfig {
        applicationId = "com.example.pinpad_app"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    packaging {
        jniLibs {
            keepDebugSymbols.add("**/libclisitef.so")
            useLegacyPackaging = true
        }
        resources {
            excludes.addAll(listOf(
                "META-INF/*",
                "META-INF/io.netty.versions.properties",
                "META-INF/proguard/androidx-*.pro"
            ))
        }
    }
}

flutter {
    source = "../.."
}

dependencies {
    implementation(files("libs/clisitef-android.jar"))
    implementation("com.google.zxing:core:3.4.1")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0@aar")
    implementation("com.google.android.material:material:1.9.0")
    implementation("org.slf4j:slf4j-api:1.7.25")
}
