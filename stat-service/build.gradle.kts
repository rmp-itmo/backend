plugins {
    id("buildlogic.kotlin-service-conventions")
}

group = "com.rmp.stat"
version = "0.0.1"

dependencies {
    implementation(project(":lib"))
}

val mainClassName = "com.rmp.stat.AppKt"

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
        delete("$rootDir/docker/jvm/dist/stat.jar")
        copy {
            from("$rootDir/stat-service/build/libs/stat-service-$version-all.jar")
            into("$rootDir/docker/jvm/dist")
            rename {
                "stat.jar"
            }
        }
    }
}