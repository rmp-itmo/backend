val jdbcClickhouseVersion: String by project

plugins {
    id("buildlogic.kotlin-service-conventions")
}

group = "com.rmp.logger"
version = "0.0.1"

dependencies {
    implementation(project(":lib"))
    implementation("com.clickhouse:clickhouse-jdbc:$jdbcClickhouseVersion")
}

val mainClassName = "com.rmp.logger.AppKt"

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
        delete("$rootDir/docker/jvm/dist/logger.jar")
        println(rootDir)
        copy {
            from("$rootDir/logger-service/build/libs/logger-service-$version-all.jar")
            into("$rootDir/docker/jvm/dist")
            rename {
                "logger.jar"
            }
        }
    }
}