import gradle.kotlin.dsl.accessors._2c3af771d8bff67d86217fd21a9af7e3.implementation

val kotlinVersion: String by project
val kotlinxCoroutinesVersion: String by project
val kotlinxSerializationVersion: String by project

val kodeinVersion: String by project

val logbackVersion: String by project
val prometeusVersion: String by project

val typesafeVersion: String by project
val config4kVersion: String by project

val jbcryptVersion: String by project
val kredsVersion: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
}

dependencies {
    fun jetBrains(module: String, version: String) = "org.jetbrains.$module:$version"
    fun kotlin(module: String) = jetBrains("kotlin:kotlin-$module", kotlinVersion)

    //Config
    implementation("com.typesafe:config:$typesafeVersion")
    implementation("io.github.config4k:config4k:$config4kVersion")

    //DI
    implementation("org.kodein.di:kodein-di-jvm:$kodeinVersion")

    //Tests
    testImplementation(kotlin("test-junit"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    //Logging and metrics
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$prometeusVersion")

    //Kotlinx coroutines
    runtimeOnly(kotlin("reflect", kotlinVersion))
    implementation(kotlin("reflect", kotlinVersion))
    implementation(jetBrains("kotlinx:kotlinx-coroutines-core", kotlinxCoroutinesVersion))

    implementation(jetBrains("kotlinx:kotlinx-serialization-json", kotlinxSerializationVersion))

    //Crypto
    implementation("org.mindrot:jbcrypt:$jbcryptVersion")

    implementation("io.github.crackthecodeabhi:kreds:$kredsVersion")

    implementation("org.reflections:reflections:0.9.12")

    implementation("redis.clients:jedis:5.2.0")
}


java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

//tasks.register("test", Test::class)

tasks.named<Test>("test").configure {
    useJUnitPlatform()
}

tasks.named("build").configure {

}