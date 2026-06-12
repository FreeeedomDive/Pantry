plugins {
    id("pantry.kotlin-common")
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
}

val libs = the<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.findVersion("spring-boot").get()}")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xannotation-default-target=param-property")
    }
}
