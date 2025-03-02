val ktorVersion: String by project
val kotlinVersion: String by project

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    println("KOTLIN VERSION $kotlinVersion")
    println("KTOR VERSION $ktorVersion")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("io.ktor.plugin:plugin:$ktorVersion")
    implementation("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
}