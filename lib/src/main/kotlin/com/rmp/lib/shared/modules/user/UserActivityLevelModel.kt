package com.rmp.lib.shared.modules.user

import com.rmp.lib.utils.korm.IdTable

object UserActivityLevelModel: IdTable("user_activity_level_model") {
    val name = text("activity_name")
    val caloriesCoefficient = float("calories_coefficient")
    val waterCoefficient = float("water_coefficient")
}