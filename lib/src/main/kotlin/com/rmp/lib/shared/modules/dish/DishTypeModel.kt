package com.rmp.lib.shared.modules.dish

import com.rmp.lib.utils.korm.IdTable

object DishTypeModel: IdTable("dish_type") {
    val name = text("name")
}