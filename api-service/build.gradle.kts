plugins {
    id("buildlogic.kotlin-api-conventions")
}

group = "com.rmp.api"
version = "0.0.1"

dependencies {
    implementation(project(":lib"))
}

application {
    mainClass.set("com.rmp.api.ApplicationKt")
}

tasks.named("build") {
    doLast {
        delete("$rootDir/docker/jvm/dist/api.jar")
        copy {
            from("$rootDir/api-service/build/libs/api-service-all.jar")
            into("$rootDir/docker/jvm/dist")
            rename {
                "api.jar"
            }
        }
    }
}