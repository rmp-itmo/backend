package com.rmp.lib.shared.modules.user

import com.rmp.lib.shared.modules.sleep.SleepQualityModel
import com.rmp.lib.utils.korm.IdTable

object UserSleepModel: IdTable("user_sleep") {
    val userId = reference("user_id", UserModel)
    val sleepHours = int("sleep_hours")
    val sleepMinutes = int("sleep_minutes")
    val sleepQuality = reference("sleep_quality", SleepQualityModel)
    val date = int("date")
}