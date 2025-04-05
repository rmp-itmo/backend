package com.rmp.loader.dto.menu


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MenuDto(
    @SerialName("meals")
    val meals: List<Meal>,
    @SerialName("params")
    val params: ParamsDto
) {
    @Serializable
    data class Meal(
        @SerialName("dishes")
        val dishes: List<Dish>,
        @SerialName("mealId")
        val mealId: Long,
        @SerialName("name")
        val name: String,
        @SerialName("params")
        val params: ParamsDto
    ) {
        @Serializable
        data class Dish(
            @SerialName("calories")
            val calories: Double,
            @SerialName("carbohydrates")
            val carbohydrates: Double,
            @SerialName("checked")
            val checked: Boolean,
            @SerialName("description")
            val description: String,
            @SerialName("fat")
            val fat: Double,
            @SerialName("id")
            val id: Int,
            @SerialName("imageUrl")
            val imageUrl: String,
            @SerialName("menuItemId")
            val menuItemId: Int,
            @SerialName("name")
            val name: String,
            @SerialName("portionsCount")
            val portionsCount: Int,
            @SerialName("protein")
            val protein: Double,
            @SerialName("timeToCook")
            val timeToCook: Int,
            @SerialName("typeId")
            val typeId: Int
        )
    }
}