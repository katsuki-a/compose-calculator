plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}
kotlin { jvmToolchain(17) }
java { withSourcesJar() }
dependencies { testImplementation(libs.junit) }
publishing {
    repositories {
        maven {
            name = "Distribution"
            url = rootProject.layout.projectDirectory.dir("distribution/maven").asFile.toURI()
        }
    }
    publications {
        create<MavenPublication>("release") { from(components["java"]) }
    }
}
