plugins {
    id("buildlogic.kotlin-service-conventions")
}

group = "com.rmp.tm"
version = "0.0.1"

dependencies {
    implementation(project(":lib"))
}

application {
    mainClass.set("com.rmp.tm.AppKt")
}

tasks.named("build") {
    doLast {
        delete("$rootDir/docker/jvm-services/dist/tm.jar")
        copy {
            from("$rootDir/tm-service/build/libs/tm-service-all.jar")
            into("$rootDir/docker/jvm/dist")
            rename {
                "tm.jar"
            }
        }
    }
}