package com.rmp.lib.shared.conf

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract

object AppConf {
    private val mainConfig: Config = ConfigFactory.load()

    const val DB_REQUEST_PROCESSING_TIME_METRIC = "db-request-processing-time"

    val isDebug: Boolean = mainConfig.getValue("application.isDebug").unwrapped().toString().toBoolean()

    val fileLocation: String = mainConfig.getValue("application.fileLocation").unwrapped().toString()

    val jwt: JwtConf = mainConfig.extract<JwtConf>("application.jwt")

    val redis: RedisConf = mainConfig.extract<RedisConf>("application.redis")

    val logger: LoggerConf = mainConfig.extract<LoggerConf>("application.loggerConf")

    val zoneOffset: Int = mainConfig.getValue("application.zoneOffset").unwrapped().toString().toInt()

    val sleepTarget: Int = mainConfig.getValue("application.sleepTarget").unwrapped().toString().toInt()

    val metrics: Boolean = mainConfig.getBoolean("application.metrics")

    val metricsPort: Int = mainConfig.getInt("application.metricsPort")
}
