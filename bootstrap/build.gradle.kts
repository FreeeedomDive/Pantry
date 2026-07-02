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
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.telegrambots.longpolling)
    implementation(libs.telegrambots.client)

    developmentOnly(libs.spring.boot.docker.compose)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
}

tasks.bootRun {
    workingDir = rootProject.projectDir
    args("--spring.profiles.active=local")
}
