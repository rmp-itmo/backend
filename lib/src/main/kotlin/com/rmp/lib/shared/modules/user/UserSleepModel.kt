package com.rmp.lib.shared.modules.user

import com.rmp.lib.utils.korm.IdTable

object UserSleepModel: IdTable("user_sleep") {
    val userId = reference("user_id", UserModel)
    val sleepHours = int("sleep_hours")
    val sleepMinutes = int("sleep_minutes")
    val date = int("date")
}