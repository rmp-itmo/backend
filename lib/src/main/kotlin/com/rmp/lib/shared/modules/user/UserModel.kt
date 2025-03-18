package com.rmp.lib.shared.modules.user

import com.rmp.lib.utils.korm.IdTable

object UserModel: IdTable("user_model") {
    val name = text("name")

    val email = text("email")
    val password = text("password")

    val waterTarget = double("water_target")
    val caloriesTarget = double("calories_target")

    val waterStreak = int("water_streak").default(0)
    val caloriesStreak = int("calories_streak").default(0)

    val height = float("height")
    val weight = float("weight")
    val activityLevel = reference("activity_level", UserActivityLevelModel)
    val goalType = reference("goal_type", UserGoalTypeModel)
    val isMale = bool("is_male")
    val age = int("age")

}