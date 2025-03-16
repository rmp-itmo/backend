package com.rmp.lib.shared.modules.diet

import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.IdTable

object DietDishLogModel: IdTable("diet_dish_log_model") {
    val time = long("time")
    val userId = reference("user_id", UserModel)
    val dish = reference("dish", DishModel)
}