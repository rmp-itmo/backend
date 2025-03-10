plugins {
    id("buildlogic.kotlin-service-conventions")
}

group = "com.rmp.diet"
version = "0.0.1"

dependencies {
    implementation(project(":lib"))
}

val mainClassName = "com.rmp.diet.AppKt"

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
        delete("$rootDir/docker/jvm/dist/diet.jar")
        copy {
            from("$rootDir/diet-service/build/libs/diet-service-$version-all.jar")
            into("$rootDir/docker/jvm/dist")
            rename {
                "diet.jar"
            }
        }
    }
}