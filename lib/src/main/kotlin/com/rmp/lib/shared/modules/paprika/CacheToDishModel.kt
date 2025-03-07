package com.rmp.lib.shared.modules.paprika

import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.utils.korm.IdTable

object CacheToDishModel: IdTable("paprika_cache") {
    val mealCache = reference("meal_cache", CacheModel)
    val dish = reference("dish", DishModel)
}