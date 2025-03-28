package com.rmp.lib.shared.modules.training

import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.IdTable

object UserTrainingLogModel: IdTable("user_training_log_model") {
    val user = reference("user_id", UserModel)
    val startAt = int("start_at")
    val endAt = int("end_at")
    val date = int("date")
    val type = reference("type", TrainingTypeModel)
    val intensity = reference("intensity", TrainingIntensityModel)
    val calories = double("calories")
}