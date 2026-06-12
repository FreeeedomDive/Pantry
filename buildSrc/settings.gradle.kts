rootProject.name = "buildSrc"

// Make the root version catalog visible to buildSrc and its convention plugins,
// so plugin/library versions are declared once in gradle/libs.versions.toml.
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
