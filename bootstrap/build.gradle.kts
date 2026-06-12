plugins {
    id("pantry.spring-conventions")
    id("pantry.testing")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":infrastructure"))
    implementation(project(":application"))
    implementation(project(":domain"))

    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.validation)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
}
