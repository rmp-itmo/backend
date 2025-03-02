val kotlinVersion: String by project

val hikaricpVersion: String by project
val jdbcPostgresVersion: String by project

plugins {
    id("buildlogic.kotlin-common-conventions")

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
    fun jetBrains(module: String, version: String) = "org.jetbrains.$module:$version"
    fun kotlin(module: String) = jetBrains("kotlin:kotlin-$module", kotlinVersion)

    implementation("com.zaxxer:HikariCP:$hikaricpVersion")
    implementation("org.postgresql:postgresql:$jdbcPostgresVersion")
}
