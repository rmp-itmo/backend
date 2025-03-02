package com.rmp.lib.shared.conf

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import io.github.config4k.getValue

object AppConf {
    private val mainConfig: Config = ConfigFactory.load()

    val isDebug: Boolean by mainConfig

    val fileLocation: String by mainConfig

    val jwt: JwtConf = mainConfig.extract<JwtConf>("application.jwt")

    val redis: RedisConf = mainConfig.extract<RedisConf>("application.redis")
}