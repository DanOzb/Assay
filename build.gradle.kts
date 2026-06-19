plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(libs.clikt)
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}