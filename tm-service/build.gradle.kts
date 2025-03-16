plugins {
    id("buildlogic.kotlin-service-conventions")
}

group = "com.rmp.tm"
version = "0.0.1"

dependencies {
    implementation(project(":lib"))
}

val mainClassName = "com.rmp.tm.AppKt"

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
        delete("$rootDir/docker/jvm/dist/tm.jar")
        copy {
            from("$rootDir/tm-service/build/libs/tm-service-$version-all.jar")
            into("$rootDir/docker/jvm/dist")
            rename {
                "tm.jar"
            }
        }
    }
}