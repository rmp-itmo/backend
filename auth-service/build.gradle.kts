plugins {
    id("buildlogic.kotlin-service-conventions")
}

group = "com.rmp.auth"
version = "0.0.1"

dependencies {
    implementation(project(":lib"))
}

application {
    mainClass.set("com.rmp.auth.AppKt")
}

tasks.named("build") {
    doLast {
        delete("$rootDir/docker/jvm-services/dist/auth.jar")
        copy {
            from("$rootDir/user-service/build/libs/auth-service-all.jar")
            into("$rootDir/docker/jvm/dist")
            rename {
                "auth.jar"
            }
        }
    }
}