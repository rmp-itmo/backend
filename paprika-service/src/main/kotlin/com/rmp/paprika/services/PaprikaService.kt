package com.rmp.paprika.services

import com.rmp.lib.exceptions.CantSolveException
import com.rmp.lib.shared.modules.auth.dto.AuthorizedUser
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.utils.korm.Row
import com.rmp.lib.utils.redis.fsm.FsmService
import com.rmp.paprika.dto.PaprikaInputDto
import com.rmp.paprika.dto.PaprikaOutputDto
import com.rmp.paprika.dto.dish.DishDto
import com.rmp.paprika.dto.dish.MacronutrientsDto
import com.rmp.paprika.dto.meal.MealOutputDto
import org.kodein.di.DI
import org.kodein.di.instance

class PaprikaService(di: DI) : FsmService(di) {
    private val dishService: DishService by instance()
    private val cacheService: CacheService by instance()
    /*
        That param is used to auto calculate and validate params
        0.25 means that the delta of generated (or provided) params must be in range of [ calories * 0.75, calories * 1.25 ]
        In the other words it means amount of acceptable calculation error
     */
    private val solverDelta = 0.25

    private fun List<Row>.countMacronutrients(): MacronutrientsDto =
        map {
            MacronutrientsDto(
                calories = it[DishModel.calories],
                protein = it[DishModel.protein],
                fat = it[DishModel.fat],
                carbohydrates = it[DishModel.carbohydrates]
            )
        }.reduce {
                a, b -> run {
                    MacronutrientsDto(
                        calories = a.calories + b.calories,
                        protein = a.protein + b.protein,
                        fat = a.fat + b.fat,
                        carbohydrates = a.carbohydrates + b.carbohydrates,
                    )
                }
        }

    fun List<Row>.toDto(): List<DishDto> = listOf()

    /*

        Solve eating method works recursively, due to optimization we need to separate big amounts of Dishes into small batches
        (here we use 750 items in a row) this approach helps to minimize the algorithm work time.

        So to solve the eating which have amount of available dishes grater than 750
        we need recursion to invoke the same solving with different batches

        To provide to the method information about what batch should it process in current call we use offset argument by default it zero
        which means "take the first batch".

        When the offset 0 besides start solving we also must check cache and if in the database we have already had the eating that user need,
        and it is we simply return it.
     */
    private fun solveEating(
        paprikaInputDto: PaprikaInputDto,
        index: Int,
        maxima: Int = 0,
        offset: Long = 1
    ): Pair<MealOutputDto, Int?> {
        println("Algorithm input: $paprikaInputDto")
        var dishesCount = 0
        if (offset.toInt() == 1) {
            val cache = cacheService.findEating(paprikaInputDto, index)
//            if (cache != null)
//                return cache

            println("Cache returns nothing :( start solving")
            // TODO: Move to next state
            //dishesCount = dishService.getDishesIdByEatingParams(paprikaInputDto.eatings[index], paprikaInputDto).count()

            //println(dishesCount)
        }

        val dishes = dishService.getDishesByEatingParams(paprikaInputDto.eatings[index], paprikaInputDto, offset)
        val eatingOptions = paprikaInputDto.eatings[index]
        val processedData = ParamsManager.process {
            withSize(eatingOptions.size)
            fromPaprikaInput(paprikaInputDto, solverDelta)
        }
        println("Processed Data ${processedData.calculatedFromParams}")
        val params = processedData.params

        if (dishesCount == 0 && maxima == 0)
            throw CantSolveException()

        //Here we are init the solver
        val solver = MPSolverService.initSolver {
            answersCount(paprikaInputDto.eatings[index].dishCount ?: 0)

            setConstraint {
                name = "Calories"
                bottom = params.calories * (1.0 - solverDelta)
                top = params.calories * (1.0 + solverDelta)
                modelKey = DishModel.calories
            }
            //We set constraints to params only if they are provided
            if (processedData.calculatedFromParams) {
                setConstraint {
                    name = "Protein"
                    bottom = params.minProtein
                    top = params.maxProtein
                    modelKey = DishModel.protein
                }
                setConstraint {
                    name = "Fat"
                    bottom = params.minFat
                    top = params.maxFat
                    modelKey = DishModel.fat
                }
                setConstraint {
                    name = "Carbohydrates"
                    bottom = params.minCarbohydrates
                    top = params.maxCarbohydrates
                    modelKey = DishModel.carbohydrates
                }
            }
//          Here was the cellulose constraint
//            setConstraint {
//                name = "Cellulose"
//                bottom = params.minCellulose
//                top = params.maxCellulose
//                modelKey = DishModel.cellulose
//            }



            //TODO: Do it on the next fsm state

            // Provide the data that will be used for calculating
//            onData(dishes)
            // Provide the objective, algorithm will be trying to optimize the calculation based of that variable
//            withObjective(DishModel.timeToCook)
            // We set direction to minimize so in that case algorithm will be trying to found the dish with less time to cook
//            onDirection(MPSolverService.SolveDirection.MINIMIZE)
        }

        //TODO: Move to next state

        val result = solver.solve()
        if (result.isEmpty()) {
            if ((maxima != 0 || dishesCount == 0) && maxima < offset * 750)
                throw CantSolveException()
            else {
                val nextMaxima = if (maxima == 0)
                    dishesCount
                else
                    maxima

                return solveEating(paprikaInputDto, index, nextMaxima, offset + 1)
            }
        }

        val micronutrients = result.countMacronutrients()

        return Pair(MealOutputDto(
            name = eatingOptions.name,
            idealParams = MacronutrientsDto(
                calories = params.calories,
                protein = params.maxProtein,
                fat = params.maxFat,
                carbohydrates = params.maxCarbohydrates,
            ),
            dishes = result.toDto(),
            params = micronutrients
        ), null)
    }

    /*

        That method is used to solve the whole daily diet,
        it simply calls the "solve eating" method for each eating in the requested diet and then processed it.

        After the eating solving we save it in cache (in the database) for future solvings
     */
    fun calculateMenu(authorizedUser: AuthorizedUser, paprikaInputDto: PaprikaInputDto): PaprikaOutputDto {
        val eatings = List(paprikaInputDto.eatings.size) {
            index ->  run {
                val eatingOutputDto = solveEating(paprikaInputDto, index)
//                eatingOutputDto.first.dishes = eatingOutputDto.first.dishes.appendIngredients()

                val cacheId = if (eatingOutputDto.second == null)
                    cacheService.saveEating(eatingOutputDto.first, paprikaInputDto, index)
                else
                    eatingOutputDto.second

                cacheService.saveUserDiet(authorizedUser.id, paprikaInputDto.eatings[index].name, cacheId!!)

                eatingOutputDto.first
            }
        }

        val params = eatings.mapIndexed { index, item ->
            item.params
        }.reduce {
            a, b -> run {
                MacronutrientsDto(
                    calories = a.calories + b.calories,
                    protein = a.protein + b.protein,
                    fat = a.fat + b.fat,
                    carbohydrates = a.carbohydrates + b.carbohydrates,
                )
            }
        }

        return PaprikaOutputDto(
            diet = paprikaInputDto.diet,
            eatings = eatings,
            params = params,
            idealParams = ParamsManager.process {
                fromPaprikaInput(paprikaInputDto, solverDelta)
            }.params
        )
    }
}