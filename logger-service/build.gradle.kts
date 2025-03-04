plugins {
    id("buildlogic.kotlin-service-conventions")
}

group = "com.rmp.logger"
version = "0.0.1"

dependencies {
    implementation(project(":lib"))
    implementation("com.clickhouse:clickhouse-jdbc:0.8.2")
}

application {
    mainClass.set("com.rmp.logger.AppKt")
}

tasks.named("build") {
    doLast {
        delete("$rootDir/docker/jvm-services/dist/logger.jar")
        copy {
            from("$rootDir/user-service/build/libs/logger-service-all.jar")
            into("$rootDir/docker/jvm/dist")
            rename {
                "logger.jar"
            }
        }
    }
}