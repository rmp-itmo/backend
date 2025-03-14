package com.rmp.lib.shared.modules.dish

import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.IdTable

object UserMenuItem: IdTable("user_menu_item") {
    val userId = reference("user_id", UserModel)
    val dishId = reference("dish_id", DishModel)
    val mealId = long("meal_id")
}