package com.rmp.paprika.services

import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.CantSolveException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.shared.modules.paprika.CacheModel
import com.rmp.lib.shared.modules.paprika.CacheToDishModel
import com.rmp.lib.utils.korm.Row
import com.rmp.lib.utils.korm.query.batch.newAutoCommitTransaction
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.RedisEventState
import com.rmp.lib.utils.redis.fsm.FsmService
import com.rmp.paprika.actions.cache.UpdateCacheState
import com.rmp.paprika.actions.meal.GenerateMealState
import com.rmp.paprika.actions.menu.GenerateMenuState
import com.rmp.paprika.dto.GenerateMenuStateDto
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

    private fun List<Row>.toDto(): List<DishDto> = map {
        DishDto(
            it[DishModel.id],
            it[DishModel.name],
            it[DishModel.imageUrl],
            it[DishModel.calories],
            it[DishModel.protein],
            it[DishModel.fat],
            it[DishModel.carbohydrates],
            it[DishModel.cookTime],
            it[DishModel.type]
        )
    }

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

    // Menu generation
    private fun buildPaprikaOutput(paprikaInputDto: PaprikaInputDto, meals: List<MealOutputDto>): PaprikaOutputDto {
        val params = meals.mapIndexed { index, item ->
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
            eatings = meals,
            params = params,
            idealParams = ParamsManager.process {
                fromPaprikaInput(paprikaInputDto, solverDelta)
            }.params
        )
    }

    private suspend fun updateCache(redisEvent: RedisEvent, state: GenerateMenuStateDto) {
        val updateCache = redisEvent.copyId("update-cache")
        updateCache.switchOn(state, AppConf.redis.paprika, RedisEventState(UpdateCacheState.SAVE_CACHE))
    }

    suspend fun init(redisEvent: RedisEvent) {
        val paprikaInputDto = redisEvent.parseData<PaprikaInputDto>() ?: throw BadRequestException("Incorrect input provided")

        redisEvent.switch(
            AppConf.redis.paprika,
            redisEvent.mutateState(GenerateMenuState.GENERATE_MEAL, GenerateMenuStateDto(paprikaInputDto)))
    }

    suspend fun generateMeal(redisEvent: RedisEvent) {
        val state = redisEvent.parseState<GenerateMenuStateDto>() ?: throw InternalServerException("Event state corrupted")

        if (state.index >= state.paprikaInputDto.meals.lastIndex)
            redisEvent.switch(redisEvent.mutateState(GenerateMenuState.MEAL_GENERATED))
        else {
            val generateMeal = redisEvent.copyId("generate-meal")
            redisEvent.switch(
                AppConf.redis.paprika,
                generateMeal.mutateState(GenerateMealState.INIT, state.clearMealState())
            )
        }
    }

    suspend fun mealGenerated(redisEvent: RedisEvent) {
        val state = redisEvent.parseState<GenerateMenuStateDto>() ?: throw InternalServerException("Event state corrupted")

        if (state.index >= state.paprikaInputDto.meals.lastIndex) {
            if (state.generated.size < state.paprikaInputDto.meals.size)
                redisEvent.switchOnApi(
                    CantSolveException("Solution not found")
                )
            else {
                updateCache(redisEvent, state)
                redisEvent.switchOnApi(
                    buildPaprikaOutput(state.paprikaInputDto, state.generated)
                )
            }
            return
        } else {
            redisEvent.switch(
                AppConf.redis.paprika,
                redisEvent.mutateState(GenerateMenuState.GENERATE_MEAL, state.nextMeal())
            )
        }


    }



    // Meal generation
    private suspend fun switchMealSolvingToDishFetch(redisEvent: RedisEvent, state: GenerateMenuStateDto) {
        val transaction = newAutoCommitTransaction {
            this add dishService
                .getDishesByMealParams(state.currentMealOptions, state.paprikaInputDto, state.offset)
                .named("dishes")
            this add dishService
                .getDishesIdByEatingParams(state.currentMealOptions, state.paprikaInputDto)
                .count("dishes-count")
        }

        redisEvent.switchOnDb(transaction, redisEvent.mutateState(GenerateMealState.SEARCH_DISHES))
    }

    private suspend fun mealSolutionFound(redisEvent: RedisEvent, paramsManager: ParamsManager, state: GenerateMenuStateDto, dishes: List<Row>, cacheId: Long? = null) {
        val mealOutput = MealOutputDto(
            state.currentMealOptions.name,
            idealParams = MacronutrientsDto(
                calories = paramsManager.params.calories,
                protein = paramsManager.params.maxProtein,
                fat = paramsManager.params.maxFat,
                carbohydrates = paramsManager.params.maxCarbohydrates,
            ),
            dishes = dishes.toDto(),
            params = dishes.countMacronutrients(),
            cacheId = cacheId
        )

        val mealSolved = redisEvent.copyId("generate-menu")

        mealSolved.switch(
            AppConf.redis.paprika,
            redisEvent.mutateState(GenerateMenuState.MEAL_GENERATED, state.appendMeal(mealOutput))
        )
    }

    suspend fun initMealSolution(redisEvent: RedisEvent) {
        val state = redisEvent.parseState<GenerateMenuStateDto>() ?: throw InternalServerException("Event state corrupted")

        if (state.offset == 1L) {
            val transaction = newAutoCommitTransaction {
                this add cacheService
                            .findIncompatible(state.paprikaInputDto.excludeDishes)
                            .named("excluded-cache-entries")

                this add cacheService
                            .findMeal(state.paprikaInputDto, state.index)
                            .named("cache-entries")
            }
            redisEvent.switchOnDb(transaction, redisEvent.mutateState(GenerateMealState.CACHE_FETCHED))
            return
        }
        switchMealSolvingToDishFetch(redisEvent, state)
    }

    suspend fun cacheFetched(redisEvent: RedisEvent) {
        val state = redisEvent.parseState<GenerateMenuStateDto>() ?: throw InternalServerException("Event state corrupted")
        val cacheEntries = redisEvent.parseDb()["cache-entries"]
        val excludedEntries = redisEvent.parseDb()["excluded-cache-entries"]?.map {
            it[CacheToDishModel.mealCache]
        } ?: listOf()

        if (cacheEntries.isNullOrEmpty())
            switchMealSolvingToDishFetch(redisEvent, state)
        else {
            val acceptableEntry = cacheEntries.firstOrNull {
                it[CacheModel.id] !in excludedEntries
            } ?: return redisEvent.switch(AppConf.redis.paprika, redisEvent.mutateState(GenerateMealState.SEARCH_DISHES))

            val transaction = newAutoCommitTransaction {
                this add cacheService
                            .getMealDishes(acceptableEntry[CacheModel.id])
                            .named("meal-dishes")
            }

            redisEvent.switchOnDb(
                transaction,
                redisEvent.mutateState(
                    GenerateMealState.SOLVED_BY_CACHE,
                    state.tempCacheId(acceptableEntry[CacheModel.id])
                )
            )
        }
    }

    suspend fun solvedByCache(redisEvent: RedisEvent) {
        val state = redisEvent.parseState<GenerateMenuStateDto>() ?: throw InternalServerException("Event state corrupted")
        val mealDishes = redisEvent.parseDb()["meal-dishes"] ?: listOf()
        val mealOptions = state.currentMealOptions

        val processed = ParamsManager.process {
            withSize(mealOptions.size)
            fromPaprikaInput(state.paprikaInputDto, solverDelta)
        }

        mealSolutionFound(redisEvent, processed, state, mealDishes, state.cacheId)
    }

    suspend fun searchDishes(redisEvent: RedisEvent) {
        val state = redisEvent.parseState<GenerateMenuStateDto>() ?: throw InternalServerException("Event state corrupted")
        val dishesCount = redisEvent.parseDb()["dishes-count"]?.firstOrNull().let {
            if (it == null) 0L
            else it[DishModel.entityCount]
        }

        if (dishesCount == 0L) {
            redisEvent.switchOnApi(CantSolveException())
            return
        }

        redisEvent.switch(
            AppConf.redis.paprika,
            redisEvent.mutateState(GenerateMealState.DISHES_FETCHED, state.setDishCount(dishesCount))
        )
    }

    suspend fun dishesFetched(redisEvent: RedisEvent) {
        val state = redisEvent.parseState<GenerateMenuStateDto>() ?: throw InternalServerException("Event state corrupted")
        val dishes = redisEvent.parseDb()["dishes"] ?: listOf()

        val processedData = ParamsManager.process {
            withSize(state.currentMealOptions.size)
            fromPaprikaInput(state.paprikaInputDto, solverDelta)
        }

        val params = processedData.params
        val solver = MPSolverService.initSolver {
            answersCount(state.currentMealOptions.dishCount ?: 0)

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

            // Provide the data that will be used for calculating
            onData(dishes)
            // Provide the objective, algorithm will be trying to optimize the calculation based of that variable
            withObjective(DishModel.cookTime)
            // We set direction to minimize so in that case algorithm will be trying to found the dish with less time to cook
            onDirection(MPSolverService.SolveDirection.MINIMIZE)
        }
        val result = solver.solve()

        if (result.isEmpty()) {
            if (state.dishesCount < state.offset * 750) {
                redisEvent.switchOnApi(CantSolveException())
            } else {
                val transaction = newAutoCommitTransaction {
                    this add dishService
                        .getDishesByMealParams(state.currentMealOptions, state.paprikaInputDto, state.offset)
                        .named("dishes")
                }
                redisEvent.switchOnDb(transaction, redisEvent.mutateState(state.nextDishBatch()))
            }
            return
        }

        mealSolutionFound(redisEvent, processedData, state, result)
    }
}