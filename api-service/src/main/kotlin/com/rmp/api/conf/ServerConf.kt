package com.rmp.api.conf

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*

object ServerConf {
    private val conf = HoconApplicationConfig(ConfigFactory.load().getConfig("server"))
    val host: String = conf.tryGetString("host") ?: throw InternalError("Host config not found")
    val port: Int = conf.tryGetString("port")?.toInt() ?: throw InternalError("Host config not found")
}