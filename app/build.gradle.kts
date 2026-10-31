plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.example.vagor"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.jabbott_iii.vagor"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Google Play needs the bundle signed with an upload key. The key comes only from environment
    // variables (locally or as CI secrets), so the keystore and its passwords never enter git.
    // Without them the release build is unsigned, as before.
    val uploadKeystore = System.getenv("VAGOR_UPLOAD_KEYSTORE")?.let { file(it) }?.takeIf { it.exists() }
    signingConfigs {
        if (uploadKeystore != null) {
            create("release") {
                storeFile = uploadKeystore
                storePassword = System.getenv("VAGOR_UPLOAD_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("VAGOR_UPLOAD_KEY_ALIAS")
                keyPassword = System.getenv("VAGOR_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // The migration tests (MigrationTestHelper) read the exported schemas as assets.
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

// Room exports each database version's schema to app/schemas, and they're kept in git so every
// migration can be tested against the version it upgrades from.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// detekt was added on 2026-10-31. Its baseline lists what it found in the code then, so only
// findings in newer code fail the build.
detekt {
    buildUponDefaultConfig = true
    source.setFrom("src/main/java", "src/test/java", "src/androidTest/java")
    baseline = file("detekt-baseline.xml")
}
// The IntelliJ IDEA style is the Kotlin coding conventions (kotlin.code.style=official), and it
// matched the existing code best, more so without trailing commas. The code was formatted to it
// once, on 2026-10-31, so ktlint needs no baseline. Lines wrap at detekt's 120 characters.
ktlint {
    version = libs.versions.ktlint.get()
    additionalEditorconfig.set(
        mapOf(
            "ktlint_code_style" to "intellij_idea",
            "ij_kotlin_allow_trailing_comma" to "false",
            "ij_kotlin_allow_trailing_comma_on_call_site" to "false",
            "max_line_length" to "120"
        )
    )
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.room.testing)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
}
