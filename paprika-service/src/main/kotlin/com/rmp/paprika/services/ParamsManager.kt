package com.rmp.paprika.services

import com.rmp.lib.exceptions.CantSolveException
import com.rmp.paprika.dto.PaprikaInputDto
import com.rmp.paprika.dto.meal.ParametersDto

class ParamsManager internal constructor() {
    lateinit var params: ParametersDto
    private var calories: Double = 0.0
    private var mealsCoef: Double = 1.0
    var calculatedFromParams: Boolean = false
    /*
        That param is used to auto calculate and validate params
        0.15 means that the delta of generated (or provided) params must be in range of [ calories * 0.85, calories * 1.15 ]
        In the other words it means amount of acceptable calculation error
     */
    private val calculateDelta = 0.15

    companion object {
        fun process(apply: ParamsManager.() -> Unit): ParamsManager {
            return ParamsManager().apply(apply)
        }
    }

    fun withSize(coef: Double) {
        mealsCoef = coef
    }

    fun fromPaprikaInput(paprikaInputDto: PaprikaInputDto, delta: Double = 1.0) {
        return if (paprikaInputDto.params != null) {
            fromParams(paprikaInputDto.params, delta)
        } else if (paprikaInputDto.calories != null) {
            fromCalories(paprikaInputDto.calories, delta)
        }
        else
            throw CantSolveException("You must provide either macronutrients params or calories")
    }

    private fun fromCalories(calories: Double?, delta: Double = 1.0) {
        if (calories == null)
            return
        this.calories = calories
        val protein = createMinMaxValue(4, delta)
        val fat = createMinMaxValue(9, delta)
        val carbohydrates = createMinMaxValue(4, delta)
        this.params = ParametersDto(
            calories = this.calories * mealsCoef,

            minProtein = protein.first,
            maxProtein = protein.second,

            minFat = fat.first,
            maxFat = fat.second,

            minCarbohydrates = carbohydrates.first,
            maxCarbohydrates = carbohydrates.second,
        )
    }
    private fun fromParams(params: ParametersDto?, delta: Double = 0.0) {
        calculatedFromParams = true
        if (params == null)
            return
        this.validateParams(params)
        this.params = ParametersDto(
            calories = params.calories * mealsCoef,

            minProtein = params.minProtein * mealsCoef * (1.0 - delta),
            maxProtein = params.maxProtein * mealsCoef * (1.0 + delta),


            minFat = params.minFat * mealsCoef * (1.0 - delta),
            maxFat = params.maxFat * mealsCoef * (1.0 + delta),

            minCarbohydrates = params.minCarbohydrates * mealsCoef * (1.0 - delta),
            maxCarbohydrates = params.maxCarbohydrates * mealsCoef * (1.0 + delta),
        )
    }

    private fun transformToCalories(value: Double, multiplier: Int = 4): Double {
        return value * multiplier
    }

    private fun summariseMinParams(params: ParametersDto): Double {
        return transformToCalories(params.minProtein) + transformToCalories(params.minCarbohydrates) + transformToCalories(params.minFat, 9)
    }

    private fun summariseMaxParams(params: ParametersDto): Double {
        return transformToCalories(params.maxProtein) + transformToCalories(params.maxCarbohydrates) + transformToCalories(params.maxFat, 9)
    }

    fun validateParams(params: ParametersDto) {
        if (params.calories * (1.0 + calculateDelta) < summariseMinParams(params) || summariseMaxParams(params) > params.calories * (1.0 + calculateDelta))
            throw CantSolveException("Bad macronutrients params were provided!")
    }

    private fun createMinMaxValue(divider: Int = 4, delta: Double = 1.0): Pair<Double, Double> {
        val value = (calories / 3) / divider * mealsCoef
        return Pair(
            value * (1 - delta),
            value * (1 + delta)
        )
    }

    operator fun invoke(paprikaInputDto: PaprikaInputDto, mealCoef: Double = 1.0): ParametersDto {
        return if (paprikaInputDto.calories != null) {
            ParametersDto(
                calories = paprikaInputDto.calories * mealCoef,

                minProtein = 0.0,
                maxProtein = paprikaInputDto.calories / 4 * mealCoef,

                minFat = 0.0,
                maxFat = paprikaInputDto.calories / 9 * mealCoef,

                minCarbohydrates = 0.0,
                maxCarbohydrates = paprikaInputDto.calories / 4 * mealCoef,
            )
        } else
            ParametersDto(
                calories = paprikaInputDto.params!!.calories * mealCoef,

                minProtein = paprikaInputDto.params.minProtein * mealCoef,
                maxProtein = paprikaInputDto.params.maxProtein * mealCoef,

                minFat = paprikaInputDto.params.minFat * mealCoef,
                maxFat = paprikaInputDto.params.maxFat * mealCoef,

                minCarbohydrates = paprikaInputDto.params.minCarbohydrates * mealCoef,
                maxCarbohydrates = paprikaInputDto.params.maxCarbohydrates * mealCoef,
            )
    }
}