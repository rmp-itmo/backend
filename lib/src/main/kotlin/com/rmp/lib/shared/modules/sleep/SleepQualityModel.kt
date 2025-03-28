package com.rmp.lib.shared.modules.sleep

import com.rmp.lib.utils.korm.IdTable

object SleepQualityModel: IdTable("sleep_quality") {
    val name = text("name")
}