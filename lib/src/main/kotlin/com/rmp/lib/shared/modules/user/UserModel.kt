package com.rmp.lib.shared.modules.user

import com.rmp.lib.utils.korm.IdTable

object UserModel: IdTable("user_model") {
    val name = text("name")

    val login = text("login")
    val password = text("password")

    val waterTarget = double("water_target").nullable()
    val caloriesTarget = double("calories_target").nullable()

    val waterStreak = int("water_streak").default(0)
    val caloriesStreak = int("calories_streak").default(0)
}