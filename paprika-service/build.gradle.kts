val googleOrtoolsVersion: String by project

plugins {
    id("buildlogic.kotlin-service-conventions")
}

group = "com.rmp.paprika"
version = "0.0.1"

dependencies {
    implementation(project(":lib"))
    implementation("com.google.ortools:ortools-java:$googleOrtoolsVersion")
}

val mainClassName = "com.rmp.paprika.AppKt"

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
        delete("$rootDir/docker/jvm/dist/paprika.jar")
        copy {
            from("$rootDir/paprika-service/build/libs/paprika-service-$version-all.jar")
            into("$rootDir/docker/jvm/dist")
            rename {
                "paprika.jar"
            }
        }
    }
}