plugins {
    id("pantry.kotlin-common")
    id("pantry.testing")
    id("pantry.spring-conventions")
}

dependencies {
    api(project(":domain"))

    implementation(libs.spring.context)
    implementation(libs.spring.tx)
    implementation(libs.kotlinx.coroutines.core)
}
