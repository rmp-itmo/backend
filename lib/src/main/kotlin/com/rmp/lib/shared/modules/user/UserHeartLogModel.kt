package com.rmp.lib.shared.modules.user

import com.rmp.lib.utils.korm.IdTable

object UserHeartLogModel: IdTable("user_heart_log_model") {
    val heartRate = int("heart_rate")
    val user = reference("user_id", UserModel)
    val date = int("date")
    val time = int("time")
}