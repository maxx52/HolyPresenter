plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)

    `maven-publish`
}

group = "org.holypresenter"
version = "0.1.0"

dependencies {
    implementation(compose.runtime)
    implementation(compose.material3)

    implementation(libs.kotlinxSerializationCore)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = "org.holypresenter"
            artifactId = "platform-api"
            version = "0.1.0"
        }
    }
}
