package com.rmp.lib.shared.modules.diet

import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.IdTable

object DietDishLogModel: IdTable("diet_dish_log_model") {
    val date = int("date")
    val userId = reference("user_id", UserModel)
    val dish = reference("dish", DishModel)
    val mealId = long("meal_id")
    val mealName = text("meal_name")
}