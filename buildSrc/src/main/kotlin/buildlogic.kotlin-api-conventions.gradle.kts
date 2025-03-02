val kotlinVersion: String by project
val kotlinxCoroutinesVersion: String by project

val ktorVersion: String by project
val kotestVersion: String by project
val ktorKotestVersion: String by project
val kodeinVersion: String by project

val logbackVersion: String by project
val prometeusVersion: String by project

plugins {
    id("buildlogic.kotlin-common-conventions")
    id("io.ktor.plugin")
    kotlin("jvm")
    kotlin("plugin.serialization")

    application
}

repositories {
    mavenCentral()
}

dependencies {
    fun ktor(part: String, module: String) = "io.ktor:ktor-$part-$module-jvm:$ktorVersion"
    fun ktorServer(module: String) = ktor(part = "server", module = module)
    
    //Ktor server
    implementation(ktorServer("host-common"))
    implementation(ktorServer("status-pages"))
    implementation(ktorServer("call-logging"))
    implementation(ktorServer("call-id"))
    implementation(ktorServer("metrics-micrometer"))
    implementation(ktorServer("content-negotiation"))
    implementation(ktorServer("netty"))
    implementation(ktorServer("compression"))
    implementation(ktorServer("cors"))
    implementation(ktor("serialization", "kotlinx-json"))
    implementation(ktorServer("core"))
    implementation(ktorServer("auth"))
    implementation(ktorServer("auth-jwt"))
    implementation(ktorServer("websockets"))
    testImplementation(ktorServer("tests"))

    //Tests
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-assertions-ktor:$ktorKotestVersion")
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