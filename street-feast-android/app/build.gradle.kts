
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// ---- Load local.properties safely (no internal AGP APIs) ----
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

val supabaseUrl: String = localProps.getProperty("SUPABASE_URL") ?: ""
val supabaseAnonKey: String = localProps.getProperty("SUPABASE_ANON_KEY") ?: ""
val onesignalAppId: String = localProps.getProperty("ONESIGNAL_APP_ID") ?: ""

// ---- Plugins ----
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.google.services)
}

// ---- Android config ----
android {
    namespace = "com.streatfeast.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.streatfeast.app"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // BuildConfig constants from local.properties
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "ONESIGNAL_APP_ID", "\"$onesignalAppId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Add any debug-only flags here if needed
        }
    }

    buildFeatures {
        viewBinding = true

        buildConfig = true
    }

    // Use Java 17 (AGP 8+ requires JDK 17)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // Optional: ensure toolchain is JDK 17 even if host defaults differ
    // (Requires Gradle 8+)
    // kotlin {
    //     jvmToolchain(17)
    // }
}

// ---- Dependencies ----
dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Lifecycle & ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Fragment & Navigation
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // RecyclerView & SwipeRefresh & ViewPager2
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Supabase (via BOM) + JSON serialization
    // Supabase (BOM) + modules
    implementation(platform("io.github.jan-tennert.supabase:bom:3.2.6"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt") // was gotrue-kt in older versions
    // Ktor HTTP client engine (required for Supabase)
    implementation("io.ktor:ktor-client-okhttp:3.3.2")
// JSON serialization (unchanged)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Firebase (required for OneSignal FCM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)

    // OneSignal
    implementation("com.onesignal:OneSignal:[5.4.0,5.4.99]")

    // Room (offline cache)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("com.google.code.gson:gson:2.10.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}

ksp {
    arg("room.incremental", "true")
    arg("room.schemaLocation", "$projectDir/schemas") // <-- add this
}


