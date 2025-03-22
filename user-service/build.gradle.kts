plugins {
    id("buildlogic.kotlin-service-conventions")
}

group = "com.rmp.user"
version = "0.0.1"

dependencies {
    implementation(project(":lib"))
}

val mainClassName = "com.rmp.user.AppKt"

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
        delete("$rootDir/docker/jvm/dist/user.jar")
        copy {
            from("$rootDir/user-service/build/libs/user-service-$version-all.jar")
            into("$rootDir/docker/jvm/dist")
            rename {
                "user.jar"
            }
        }
    }
}