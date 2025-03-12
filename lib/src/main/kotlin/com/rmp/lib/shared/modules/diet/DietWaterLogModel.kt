package com.rmp.lib.shared.modules.diet

import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.IdTable

object DietWaterLogModel: IdTable("diet_water_log_model") {
    val createdAt = long("createdAt")
    val userId = reference("user_id", UserModel)
    val volume = double("volume")
}