plugins {
    id("buildlogic.kotlin-service-conventions")
}

group = "com.rmp.auth"
version = "0.0.1"

dependencies {
    implementation(project(":lib"))
}

val mainClassName = "com.rmp.auth.AppKt"

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
        delete("$rootDir/docker/jvm/dist/auth.jar")
        copy {
            from("$rootDir/auth-service/build/libs/auth-service-$version-all.jar")
            into("$rootDir/docker/jvm/dist")
            rename {
                "auth.jar"
            }
        }
    }
}