package com.rmp.loader.dto.menu


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaprikaGenerateResult(
    @SerialName("idealParams")
    val idealParams: IdealParams,
    @SerialName("meals")
    val meals: List<Meal>,
    @SerialName("params")
    val params: ParamsDto
) {
    @Serializable
    data class Meal(
        @SerialName("dishes")
        val dishes: List<Dish>,
        @SerialName("idealParams")
        val idealParams: ParamsDto,
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
            @SerialName("fat")
            val fat: Double,
            @SerialName("id")
            val id: Int,
            @SerialName("logo")
            val logo: String,
            @SerialName("name")
            val name: String,
            @SerialName("protein")
            val protein: Double,
            @SerialName("timeToCook")
            val timeToCook: Int,
            @SerialName("typeId")
            val typeId: Int
        )
    }

    @Serializable
    data class IdealParams(
        @SerialName("calories")
        val calories: Double,
        @SerialName("maxCarbohydrates")
        val maxCarbohydrates: Double,
        @SerialName("maxFat")
        val maxFat: Double,
        @SerialName("maxProtein")
        val maxProtein: Double,
        @SerialName("minCarbohydrates")
        val minCarbohydrates: Double,
        @SerialName("minFat")
        val minFat: Double,
        @SerialName("minProtein")
        val minProtein: Double
    )
}