package com.rmp.lib.shared.modules.user

import com.rmp.lib.utils.korm.IdTable

object UserStepsLogModel: IdTable("user_steps_log_model") {
    val user = reference("user_id", UserModel)
    val count = int("count")
    val date = int("date")
    val time = int("time")
}