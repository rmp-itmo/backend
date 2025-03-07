package com.rmp.lib.shared.modules.paprika

import com.rmp.lib.shared.modules.dish.DishTypeModel
import com.rmp.lib.utils.korm.IdTable

object CacheModel: IdTable("paprika_cache") {
    //Diet options
    val calories = double("calories")
    val protein = double("protein")
    val fat = double("fat")
    val carbohydrates = double("carbohydrates")

    //Eating options
    val size = double("size")
    val type = reference("type", DishTypeModel)
    val dishCount = int("dish_count")

    //System fields
    val useTimesFromLastScrap = int("use_times_from_last_scrap")
    val useTimesFromCreation = int("use_times_from_creation")
    val onRemove = bool("on_remove")
}