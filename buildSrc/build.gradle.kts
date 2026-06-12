plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(24)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.allopen)
    implementation(libs.kotlin.noarg)
    implementation(libs.spring.boot.gradle.plugin)
    implementation(libs.spring.dep.mgmt.plugin)
}
