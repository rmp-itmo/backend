package com.rmp.lib.shared.modules.user

import com.rmp.lib.utils.korm.IdTable

object UserAchievementsModel: IdTable("user_achievements_model") {
    val userId = reference("user_id", UserModel)
    val sleep = int("sleep")
    val steps = int("steps")
    val water = int("water")
    val calories = int("calories")
}