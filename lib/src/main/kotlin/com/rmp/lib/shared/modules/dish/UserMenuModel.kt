package com.rmp.lib.shared.modules.dish

import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.IdTable

object UserMenuModel: IdTable("user_menu") {
    val userId = reference("user_id", UserModel)
    val mealId = long("meal_id")
    val name = text("name")
    val index = int("index")
}