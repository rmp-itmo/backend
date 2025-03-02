package com.rmp.tm.conf

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract

object ServiceConf {
    private val serviceConf = ConfigFactory.load()

    val dbConf = serviceConf.extract<DatabaseConf>("service.dbConf")
}