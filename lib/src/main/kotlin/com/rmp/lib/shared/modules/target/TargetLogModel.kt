package com.rmp.lib.shared.modules.target

import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.IdTable

object TargetLogModel:IdTable("target_log_model") {
    val userId = reference("user_id", UserModel)
    val date = int("date")

    val waterTarget = double("water_target")
    val caloriesTarget = double("calories_target")
    val stepTarget = int("step_target")
    val sleepTarget = float("sleep_target")

}