package com.rmp.diet.services

import com.rmp.diet.actions.dish.log.DishLogEventState
import com.rmp.diet.dto.dish.CreateDishDto
import com.rmp.diet.dto.dish.DishDto
import com.rmp.diet.dto.menu.*
import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.dto.Response
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.shared.modules.dish.UserMenuModel
import com.rmp.lib.shared.modules.dish.UserMenuItem
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.dto.CurrentCaloriesOutputDto
import com.rmp.lib.shared.dto.DishLogCheckDto
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.files.FilesUtil
import com.rmp.lib.utils.korm.Row
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.column.inList
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI
import org.kodein.di.instance

class MenuService(di: DI) : FsmService(di) {
    private val dishService: DishService by instance()

    suspend fun setMenu(redisEvent: RedisEvent) {
        val state = redisEvent.parseData<MenuInputDto>() ?: throw BadRequestException("Bad data provided")
        val providedDishes = state.meals.map { it.dishes }.flatten().distinct()

        val dishes = newTransaction(redisEvent) {
            this add UserMenuItem.delete(
                UserMenuItem.userId eq redisEvent.authorizedUser!!.id
            )

            this add UserMenuModel.delete(
                UserMenuModel.userId eq redisEvent.authorizedUser!!.id
            )

            this add DishModel.select().where { DishModel.id inList providedDishes }.count("dishes")
        }["dishes"]?.firstOrNull() ?: throw BadRequestException("Bad dishes provided")

        if (providedDishes.size > dishes[DishModel.entityCount]) throw BadRequestException("Bad dishes provided")


        val insertedMenu = transaction(redisEvent) {
            this add UserMenuModel.batchInsert(state.meals) { it, idx ->
                this[UserMenuModel.userId] = redisEvent.authorizedUser!!.id
                this[UserMenuModel.mealId] = System.nanoTime()
                this[UserMenuModel.name] = it.name
                this[UserMenuModel.index] = idx
            }.named("insert-menu")

            this add UserMenuModel
                .select(UserMenuModel.mealId, UserMenuModel.index)
                .where { UserMenuModel.userId eq redisEvent.authorizedUser!!.id }
                .named("inserted-menu")
        }["inserted-menu"] ?: throw InternalServerException("Insert failed")

        val meals: List<Pair<Int, Long>> = state.meals.mapIndexed { idx, it ->
            it.dishes.map { dish ->
                idx to dish
            }
        }.flatten()

        val mealIdByIdx = List(state.meals.size) { idx ->
            idx to insertedMenu.firstOrNull { it[UserMenuModel.index] == idx }
        }.toMap()

        val menuItems = autoCommitTransaction(redisEvent) {
            this add UserMenuItem.batchInsert(meals) { it, _ ->
                this[UserMenuItem.dishId] = it.second
                this[UserMenuItem.userId] = redisEvent.authorizedUser!!.id
                this[UserMenuItem.mealId] = mealIdByIdx[it.first]!![UserMenuModel.mealId]
            }.named("menu-items-inserted")
        }["menu-items-inserted"] ?: throw InternalServerException("Insert failed")


        if (menuItems.size < state.meals.map { it.dishes }.flatten().size) {
            throw InternalServerException("Insert failed")
        }

        redisEvent.switchOnApi(Response(
            success = true,
            data = "Menu saved"
        ))
    }


    private fun List<Row>.toDto(): List<DishDto> = map {
        DishDto(
            it[DishModel.id],
            it[DishModel.name],
            it[DishModel.description],
            it[DishModel.imageUrl],
            it[DishModel.portionsCount],
            it[DishModel.calories],
            it[DishModel.protein],
            it[DishModel.fat],
            it[DishModel.carbohydrates],
            it[DishModel.cookTime],
            it[DishModel.type],
            it[UserMenuItem.id],
            it[UserMenuItem.checked]
        )
    }

    suspend fun selectMenu(redisEvent: RedisEvent) {
        val select = newAutoCommitTransaction(redisEvent) {
            this add UserMenuModel
                        .select(UserMenuModel.mealId, UserMenuModel.name)
                        .where { UserMenuModel.userId eq redisEvent.authorizedUser!!.id }
                        .named("select-menu")

            this add UserMenuItem
                        .select()
                        .join(DishModel)
                        .where { UserMenuItem.userId eq redisEvent.authorizedUser!!.id }
                        .named("select-menu-items")
        }

        val menu = select["select-menu"] ?: throw InternalServerException("Select failed")
        val menuItems = select["select-menu-items"] ?: throw InternalServerException("Select failed")

        val dishToMeal = menuItems.groupBy {
            it[UserMenuItem.mealId]
        }

        val mealById = menu.groupBy {
            it[UserMenuModel.mealId]
        }

        val meals = dishToMeal.map { (mealId, v) ->
            val dishes = v.toDto()
            MealOutputDto(
                mealId = mealId,
                name = mealById[mealId]?.first()?.get(UserMenuModel.name)!!,
                dishes = dishes,
                params = dishes.reduce { a, b ->
                    a + b
                }.let {
                    MacronutrientsDto(
                        it.calories,
                        it.protein,
                        it.fat,
                        it.carbohydrates
                    )
                }
            )
        }.toMutableList()

        // If all dishes were removed from the meal, we must add it anyway
        meals += mealById.mapNotNull { (mealId, data) ->
            if (dishToMeal.containsKey(mealId)) null
            else MealOutputDto(
                mealId = mealId,
                name = data.first()[UserMenuModel.name],
                dishes = listOf(),
                params = MacronutrientsDto(0.0, 0.0, 0.0, 0.0)
            )
        }

        val params = meals.reduceOrNull { acc, op ->
            MealOutputDto(
                mealId = acc.mealId,
                name = acc.name,
                dishes = acc.dishes,
                params = acc.params + op.params
            )
        } ?: MealOutputDto(0, "", listOf(), MacronutrientsDto(0.0, 0.0, 0.0, 0.0))

        val menuOutputDto = MenuOutputDto(
            meals = meals,
            params = params.let {
                MacronutrientsDto(
                    it.params.calories,
                    it.params.protein,
                    it.params.fat,
                    it.params.carbohydrates
                )
            }
        )

        if (meals.isEmpty()) throw BadRequestException("Menu not generated")

        redisEvent.switchOnApi(menuOutputDto)
    }


    private suspend fun createDish(redisEvent: RedisEvent, dish: CreateDishDto, userId: Long): Long {
        val imageName = FilesUtil.buildName(dish.imageName)

        val insert = transaction(redisEvent) {
            this add DishModel
                .insert {
                    it[name] = dish.name
                    it[description] = dish.description
                    it[portionsCount] = dish.portionsCount
                    it[imageUrl] = imageName
                    it[calories] = dish.calories
                    it[protein] = dish.protein
                    it[fat] = dish.fat
                    it[carbohydrates] = dish.carbohydrates
                    it[cookTime] = dish.timeToCook
                    it[author] = userId
                    it[type] = dish.typeId
                }.named("insert-dish")
        }["insert-dish"]?.firstOrNull() ?: throw InternalServerException("Insert failed")

        FilesUtil.upload(dish.image, imageName)

        return insert[DishModel.id]
    }

    private suspend fun updateUserCalories(redisEvent: RedisEvent, dietLogCheckDto: DishLogCheckDto) {
        redisEvent
            .copyId("update-calories")
            .switchOn(
                dietLogCheckDto,
                AppConf.redis.user,
                redisEvent.mutate(DishLogEventState.UPDATE_CALORIES)
            )
    }

    private suspend fun returnUserCaloriesWithoutChange(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()

        val userData = autoCommitTransaction(redisEvent) {
            this add UserModel.select(UserModel.caloriesCurrent).where {
                UserModel.id eq user.id
            }
        }[UserModel]?.firstOrNull() ?: throw InternalServerException("Failed to fetch user")

        redisEvent.switchOnApi(CurrentCaloriesOutputDto(calories = userData[UserModel.caloriesCurrent]))
    }

    suspend fun addMenuItem(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()
        val addMenuItemDto = redisEvent.parseData<AddMenuItemDto>() ?: throw BadRequestException("Bad dish create dto provided")

        //Init new transaction
        newTransaction(redisEvent) {}

        val (dishId, calories) = with(addMenuItemDto) {
            if (dishId == null && newDish != null)
                createDish(redisEvent, newDish, user.id) to newDish.calories
            else
                (dishId ?: throw BadRequestException("You must provide either new dish data or dishId")) to
                (dishService.getDishCalories(redisEvent, dishId) ?: throw BadRequestException("Dish not found"))
        }

        val addMenuItem = transaction(redisEvent) {
            this add UserMenuModel
                            .select(UserMenuModel.mealId)
                            .where { UserMenuModel.mealId eq addMenuItemDto.mealId }

            this add UserMenuItem.insert {
                it[mealId] = addMenuItemDto.mealId
                it[checked] = addMenuItemDto.check
                it[userId] = user.id
                it[UserMenuItem.dishId] = dishId
            }.named("add-menu-item")
        }


        addMenuItem[UserMenuModel]?.firstOrNull() ?: throw BadRequestException("Unknown meal id")

        val log = addMenuItem["add-menu-item"]?.firstOrNull() ?: throw InternalServerException("Insert failed")


        if (!addMenuItemDto.check) {
            returnUserCaloriesWithoutChange(redisEvent)
            return
        }

        val data = DishLogCheckDto(
            log[UserMenuItem.id],
            true,
            calories
        )

        updateUserCalories(redisEvent, data)
    }

    suspend fun removeMenuItem(redisEvent: RedisEvent) {
        val removeMenuItemDto = redisEvent.parseData<RemoveMenuItemDto>() ?: throw BadRequestException("Bad dish create dto provided")

        val menuItem = newTransaction(redisEvent) {
            this add UserMenuItem
                            .select(UserMenuItem.id, UserMenuItem.checked, DishModel.calories)
                            .join(DishModel)
                            .where { UserMenuItem.id eq removeMenuItemDto.menuItemId }
        }[UserMenuItem]?.firstOrNull() ?: throw BadRequestException("Bad menu item provided")

        transaction(redisEvent) {
            this add UserMenuItem.delete(menuItem)
        }

        if (menuItem[UserMenuItem.checked]) {
            updateUserCalories(redisEvent, DishLogCheckDto(removeMenuItemDto.menuItemId, false, menuItem[DishModel.calories]))
            return
        }

        returnUserCaloriesWithoutChange(redisEvent)
    }
}