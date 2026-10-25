// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        // AGP 9 compiles Kotlin itself (built-in Kotlin); this sets the Kotlin version it uses.
        classpath(libs.kotlin.gradlePlugin)
    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
}