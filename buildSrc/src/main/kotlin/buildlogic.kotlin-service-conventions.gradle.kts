import gradle.kotlin.dsl.accessors._62df3888b74c437c9c33a0c31fecf9fc.java

val hikaricpVersion: String by project
val jdbcPostgresVersion: String by project

plugins {
    id("buildlogic.kotlin-common-conventions")
    id("com.github.johnrengelman.shadow")

    application
}

repositories {
    mavenCentral()
}

application {
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation("com.zaxxer:HikariCP:$hikaricpVersion")
    implementation("org.postgresql:postgresql:$jdbcPostgresVersion")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.named<Test>("test").configure {
    useJUnitPlatform()
}

tasks.named("build").configure {

}