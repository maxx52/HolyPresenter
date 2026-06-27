plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)

    `maven-publish`
}

group = "org.holypresenter"
version = "0.1.0"

dependencies {
    implementation("org.jetbrains.compose.runtime:runtime:1.11.1")
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
}