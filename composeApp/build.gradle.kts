import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Read per-sport credentials from local.properties
// Keys follow the pattern: "{sport}.supabase.url", "{sport}.supabase.anon.key", etc.
val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) load(localPropsFile.inputStream())
}

// Read keystore credentials from keystore.properties
val keystoreProperties = Properties().apply {
    val keystorePropsFile = rootProject.file("keystore.properties")
    if (keystorePropsFile.exists()) load(keystorePropsFile.inputStream())
}

/** Read a sport-scoped property. E.g. sportProp("tennis", "supabase.url") → tennis.supabase.url */
fun sportProp(sport: String, key: String): String =
    localProperties.getProperty("$sport.$key", "")

android {
    namespace = "com.ashutosh.mindfultennis"
    compileSdk = 36

    defaultConfig {
        // applicationId is overridden per product flavor
        minSdk = 29
        targetSdk = 36
        versionCode = 18
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Fallback empty values — actual values are injected by each product flavor
        buildConfigField("String", "SPORT_ID", "\"tennis\"")
        buildConfigField("String", "SUPABASE_URL", "\"\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"\"")
        buildConfigField("String", "REVENUECAT_KEY", "\"\"")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProperties.getProperty("storeFile", "")
            if (storeFilePath.isNotEmpty()) {
                storeFile = file(storeFilePath)
            }
            storePassword = keystoreProperties.getProperty("storePassword", "")
            keyAlias = keystoreProperties.getProperty("keyAlias", "")
            keyPassword = keystoreProperties.getProperty("keyPassword", "")
        }
    }

    // ── Sport Product Flavors ───────────────────────────────────────────────
    //
    // One Android app per sport. Each flavor:
    //   • Sets its own applicationId (com.mindful.{sport})
    //   • Injects SPORT_ID, SUPABASE_URL, SUPABASE_ANON_KEY, REVENUECAT_KEY via BuildConfig
    //   • Reads credentials from local.properties keys: "{sport}.supabase.url" etc.
    //   • Has its own src/{sport}Main/res/values/strings.xml for the app name
    //
    // To build a specific sport: ./gradlew :composeApp:assembleTennisRelease
    // To build all:              ./gradlew :composeApp:assembleRelease

    flavorDimensions += "sport"

    productFlavors {

        create("tennis") {
            dimension = "sport"
            applicationId = "com.mindful.tennis"
            manifestPlaceholders["deepLinkScheme"] = "com.mindful.tennis"
            buildConfigField("String", "SPORT_ID", "\"tennis\"")
            buildConfigField("String", "SUPABASE_URL", "\"${sportProp("tennis", "supabase.url")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${sportProp("tennis", "supabase.anon.key")}\"")
            buildConfigField("String", "REVENUECAT_KEY", "\"${sportProp("tennis", "revenuecat.android.key")}\"")
        }

        create("badminton") {
            dimension = "sport"
            applicationId = "com.mindful.badminton"
            manifestPlaceholders["deepLinkScheme"] = "com.mindful.badminton"
            buildConfigField("String", "SPORT_ID", "\"badminton\"")
            buildConfigField("String", "SUPABASE_URL", "\"${sportProp("badminton", "supabase.url")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${sportProp("badminton", "supabase.anon.key")}\"")
            buildConfigField("String", "REVENUECAT_KEY", "\"${sportProp("badminton", "revenuecat.android.key")}\"")
        }

        create("pickleball") {
            dimension = "sport"
            applicationId = "com.mindful.pickleball"
            manifestPlaceholders["deepLinkScheme"] = "com.mindful.pickleball"
            buildConfigField("String", "SPORT_ID", "\"pickleball\"")
            buildConfigField("String", "SUPABASE_URL", "\"${sportProp("pickleball", "supabase.url")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${sportProp("pickleball", "supabase.anon.key")}\"")
            buildConfigField("String", "REVENUECAT_KEY", "\"${sportProp("pickleball", "revenuecat.android.key")}\"")
        }

        create("squash") {
            dimension = "sport"
            applicationId = "com.mindful.squash"
            manifestPlaceholders["deepLinkScheme"] = "com.mindful.squash"
            buildConfigField("String", "SPORT_ID", "\"squash\"")
            buildConfigField("String", "SUPABASE_URL", "\"${sportProp("squash", "supabase.url")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${sportProp("squash", "supabase.anon.key")}\"")
            buildConfigField("String", "REVENUECAT_KEY", "\"${sportProp("squash", "revenuecat.android.key")}\"")
        }

        create("tabletennis") {
            dimension = "sport"
            applicationId = "com.mindful.tabletennis"
            manifestPlaceholders["deepLinkScheme"] = "com.mindful.tabletennis"
            buildConfigField("String", "SPORT_ID", "\"tabletennis\"")
            buildConfigField("String", "SUPABASE_URL", "\"${sportProp("tabletennis", "supabase.url")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${sportProp("tabletennis", "supabase.anon.key")}\"")
            buildConfigField("String", "REVENUECAT_KEY", "\"${sportProp("tabletennis", "revenuecat.android.key")}\"")
        }

        create("padel") {
            dimension = "sport"
            applicationId = "com.mindful.padel"
            manifestPlaceholders["deepLinkScheme"] = "com.mindful.padel"
            buildConfigField("String", "SPORT_ID", "\"padel\"")
            buildConfigField("String", "SUPABASE_URL", "\"${sportProp("padel", "supabase.url")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${sportProp("padel", "supabase.anon.key")}\"")
            buildConfigField("String", "REVENUECAT_KEY", "\"${sportProp("padel", "revenuecat.android.key")}\"")
        }

        create("racquetball") {
            dimension = "sport"
            applicationId = "com.mindful.racquetball"
            manifestPlaceholders["deepLinkScheme"] = "com.mindful.racquetball"
            buildConfigField("String", "SPORT_ID", "\"racquetball\"")
            buildConfigField("String", "SUPABASE_URL", "\"${sportProp("racquetball", "supabase.url")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${sportProp("racquetball", "supabase.anon.key")}\"")
            buildConfigField("String", "REVENUECAT_KEY", "\"${sportProp("racquetball", "revenuecat.android.key")}\"")
        }

        create("platformtennis") {
            dimension = "sport"
            applicationId = "com.mindful.platformtennis"
            manifestPlaceholders["deepLinkScheme"] = "com.mindful.platformtennis"
            buildConfigField("String", "SPORT_ID", "\"platformtennis\"")
            buildConfigField("String", "SUPABASE_URL", "\"${sportProp("platformtennis", "supabase.url")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${sportProp("platformtennis", "supabase.anon.key")}\"")
            buildConfigField("String", "REVENUECAT_KEY", "\"${sportProp("platformtennis", "revenuecat.android.key")}\"")
        }

        create("poptennis") {
            dimension = "sport"
            applicationId = "com.mindful.poptennis"
            manifestPlaceholders["deepLinkScheme"] = "com.mindful.poptennis"
            buildConfigField("String", "SPORT_ID", "\"poptennis\"")
            buildConfigField("String", "SUPABASE_URL", "\"${sportProp("poptennis", "supabase.url")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${sportProp("poptennis", "supabase.anon.key")}\"")
            buildConfigField("String", "REVENUECAT_KEY", "\"${sportProp("poptennis", "revenuecat.android.key")}\"")
        }

        create("beachtennis") {
            dimension = "sport"
            applicationId = "com.mindful.beachtennis"
            manifestPlaceholders["deepLinkScheme"] = "com.mindful.beachtennis"
            buildConfigField("String", "SPORT_ID", "\"beachtennis\"")
            buildConfigField("String", "SUPABASE_URL", "\"${sportProp("beachtennis", "supabase.url")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${sportProp("beachtennis", "supabase.anon.key")}\"")
            buildConfigField("String", "REVENUECAT_KEY", "\"${sportProp("beachtennis", "revenuecat.android.key")}\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
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
    implementation(project(":shared"))
    implementation(libs.koin.android)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
}
