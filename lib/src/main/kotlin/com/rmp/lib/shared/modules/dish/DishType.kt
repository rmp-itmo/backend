package com.rmp.lib.shared.modules.dish

import com.rmp.lib.utils.korm.IdTable

object DishType: IdTable("dish_type") {
    val name = text("name")
}