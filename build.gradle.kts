plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://www.jetbrains.com/intellij-repository/snapshots")
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    maven("https://redirector.kotlinlang.org/maven/kotlin-ide-plugin-dependencies")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(libs.clikt)

    //analysis api
    implementation(libs.ij.core.impl)
    implementation(libs.ij.util)
    implementation(libs.ij.java.psi.impl)
    implementation(libs.kotlin.reflect)

    implementation(libs.caffeine)

    listOf(
        libs.aa.api, libs.aa.k2, libs.aa.impl.base, libs.aa.platform, libs.aa.standalone,
        libs.ll.fir, libs.slc, libs.kc.common, libs.kc.fir, libs.kc.fe10, libs.kc.ir,
    ).forEach { implementation(it) { isTransitive = false } }

    //kotest
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}