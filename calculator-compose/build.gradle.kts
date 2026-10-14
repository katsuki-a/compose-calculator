plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}
android {
    namespace = "dev.calculator.compose"
    compileSdk = 37
    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    publishing { singleVariant("release") { withSourcesJar() } }
}
kotlin { jvmToolchain(17) }
dependencies {
    api(project(":calculator-core"))
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.preview)
    debugImplementation(libs.compose.tooling)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.test.runner)
    debugImplementation(libs.compose.test.manifest)
}
afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "Distribution"
                url = rootProject.layout.projectDirectory.dir("distribution/maven").asFile.toURI()
            }
        }
        publications {
            create<MavenPublication>("release") { from(components["release"]) }
        }
    }
}
