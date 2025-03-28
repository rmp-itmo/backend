plugins {
    id("buildlogic.kotlin-service-conventions")
}

group = "com.rmp.forum"
version = "0.0.1"

dependencies {
    implementation(project(":lib"))
}

val mainClassName = "com.rmp.forum.AppKt"

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
        delete("$rootDir/docker/jvm/dist/forum.jar")
        copy {
            from("$rootDir/forum-service/build/libs/forum-service-$version-all.jar")
            into("$rootDir/docker/jvm/dist")
            rename {
                "forum.jar"
            }
        }
    }
}