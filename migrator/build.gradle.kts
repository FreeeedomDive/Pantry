plugins {
    id("pantry.spring-conventions")
    id("pantry.testing")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":schema"))

    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.kotlin.reflect)

    runtimeOnly(libs.postgresql)
}
