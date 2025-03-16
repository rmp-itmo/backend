val ktorVersion: String by project
val kotlinVersion: String by project

plugins {
    `kotlin-dsl`

    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
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