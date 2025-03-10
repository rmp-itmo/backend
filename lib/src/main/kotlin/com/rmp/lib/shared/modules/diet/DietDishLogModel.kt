package com.rmp.lib.shared.modules.diet

import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.IdTable

object DietDishLogModel: IdTable("diet_food_log_model") {
    val createdAt = long("createdAt")
    val userId = reference("user_id", UserModel)
    val dish = reference("dish", DishModel)
}