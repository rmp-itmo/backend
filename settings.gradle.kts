plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

fun service(name: String) = "$name-service"

rootProject.name = "rmp"

include("lib")
include(service("api"))
include(service("tm"))
include(service("auth"))
include(service("logger"))
include(service("paprika"))
include(service("diet"))
include(service("user"))
include(service("stat"))
