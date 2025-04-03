val ktorClientVersion: String by project

plugins {
    id("buildlogic.kotlin-service-conventions")
}

group = "com.rmp.loader"
version = "0.0.1"

dependencies {
    fun ktorClient(name: String) = "io.ktor:ktor-client-$name:$ktorClientVersion"

    implementation(ktorClient("cio"))
    implementation(ktorClient("core"))
    implementation(ktorClient("content-negotiation"))
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorClientVersion")
}

val mainClassName = "com.rmp.loader.AppKt"

application {
    mainClass.set(mainClassName)
}

tasks.shadowJar {
    manifest {
        attributes(
            "Main-Class" to mainClassName
        )
    }
}

tasks.named("build") {
    doLast {
        delete("$rootDir/docker/jvm/dist/loader.jar")
        println(rootDir)
        copy {
            from("$rootDir/loader-service/build/libs/loader-service-$version-all.jar")
            into("$rootDir/docker/jvm/dist")
            rename {
                "loader.jar"
            }
        }
    }
}