package com.rmp.lib.shared.modules.dish

import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.IdTable

object DishModel: IdTable("dish") {
    val name = text("name")
    val description = text("description")
    val portionsCount = int("portions_count")
    val imageUrl = text("image_url")
    val calories = double("calories")
    val protein = double("protein")
    val fat = double("fat")
    val carbohydrates = double("carbohydrates")
    val cookTime = int("cook_time")
    val type = reference("type", DishTypeModel)
    val author = reference("author", UserModel).nullable().default(null)
    val private = bool("private").default(true)
}