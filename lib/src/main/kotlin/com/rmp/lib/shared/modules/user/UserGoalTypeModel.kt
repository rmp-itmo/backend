package com.rmp.lib.shared.modules.user

import com.rmp.lib.utils.korm.IdTable

object UserGoalTypeModel: IdTable("user_goal_type_model") {
    val name = text("goal_name")
    val coefficient = float("coefficient")
}