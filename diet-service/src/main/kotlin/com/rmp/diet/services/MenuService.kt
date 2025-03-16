package com.rmp.diet.services

import com.rmp.diet.dto.dish.DishDto
import com.rmp.diet.dto.menu.MacronutrientsDto
import com.rmp.diet.dto.menu.MealOutputDto
import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.dto.Response
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.shared.modules.dish.UserMenuModel
import com.rmp.lib.shared.modules.dish.UserMenuItem
import com.rmp.diet.dto.menu.MenuInputDto
import com.rmp.diet.dto.menu.MenuOutputDto
import com.rmp.lib.utils.korm.Row
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI

class MenuService(di: DI) : FsmService(di) {

    suspend fun setMenu(redisEvent: RedisEvent) {
        val state = redisEvent.parseData<MenuInputDto>() ?: throw BadRequestException("Bad data provided")

        newTransaction(redisEvent) {
            this add UserMenuItem.delete(
                UserMenuItem.userId eq redisEvent.authorizedUser!!.id
            )

            this add UserMenuModel.delete(
                UserMenuModel.userId eq redisEvent.authorizedUser!!.id
            )
        }

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
            it[DishModel.type]
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
        }

        val menuOutputDto = MenuOutputDto(
            meals = meals,
            params = meals.reduce { acc, op ->
                MealOutputDto(
                    name = acc.name,
                    dishes = acc.dishes,
                    params = acc.params + op.params
                )
            }.let {
                MacronutrientsDto(
                    it.params.calories,
                    it.params.protein,
                    it.params.fat,
                    it.params.carbohydrates
                )
            }
        )

        redisEvent.switchOnApi(menuOutputDto)
    }
}