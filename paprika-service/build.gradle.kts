plugins {
    id("buildlogic.kotlin-service-conventions")
}

group = "com.rmp.paprika"
version = "0.0.1"

dependencies {
    implementation(project(":lib"))
    implementation("com.google.ortools:ortools-java:9.7.2996")
}

application {
    mainClass.set("com.rmp.paprika.AppKt")
}

tasks.named("build") {
    doLast {
        delete("$rootDir/docker/jvm-services/dist/paprika.jar")
        copy {
            from("$rootDir/user-service/build/libs/paprika-service-all.jar")
            into("$rootDir/docker/jvm/dist")
            rename {
                "paprika.jar"
            }
        }
    }
}