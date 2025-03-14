package com.rmp.diet.services

import com.rmp.diet.actions.user.menu.get.GetUserMenuEventState
import com.rmp.diet.actions.user.menu.set.SetUserMenuEventState
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
import com.rmp.lib.utils.korm.query.batch.autoCommitTransaction
import com.rmp.lib.utils.korm.query.batch.newAutoCommitTransaction
import com.rmp.lib.utils.korm.query.batch.newTransaction
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI

class MenuService(di: DI) : FsmService(di) {
    suspend fun set(redisEvent: RedisEvent) {
        val state = redisEvent.parseData<MenuInputDto>() ?: throw BadRequestException("Bad data provided")

        val transaction = newTransaction {
            this add UserMenuItem.delete(
                UserMenuItem.userId eq redisEvent.authorizedUser!!.id
            ).named("remove-menu-item")

            this add UserMenuModel.delete(
                UserMenuModel.userId eq redisEvent.authorizedUser!!.id
            ).named("remove-menu-model")

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
        }

        redisEvent.switchOnDb(transaction, redisEvent.mutate(SetUserMenuEventState.SET_DISHES, state))
    }

    suspend fun setDishes(redisEvent: RedisEvent) {
        val data = redisEvent.parseDb()["inserted-menu"] ?: throw InternalServerException("Insert failed")
        val state = redisEvent.parseState<MenuInputDto>() ?: throw BadRequestException("Bad data provided")

        val meals: List<Pair<Int, Long>> = state.meals.mapIndexed { idx, it ->
            it.dishes.map { dish ->
                idx to dish
            }
        }.flatten()

        val mealIdByIdx = List(state.meals.size) { idx ->
            idx to data.firstOrNull { it[UserMenuModel.index] == idx }
        }.toMap()

        val transaction = autoCommitTransaction {
            this add UserMenuItem.batchInsert(meals) { it, _ ->
                this[UserMenuItem.dishId] = it.second
                this[UserMenuItem.userId] = redisEvent.authorizedUser!!.id
                this[UserMenuItem.mealId] = mealIdByIdx[it.first]!![UserMenuModel.mealId]
            }.named("menu-items-inserted")
        }

        redisEvent.switchOnDb(transaction, redisEvent.mutate(SetUserMenuEventState.DISHED_SAVED))
    }

    suspend fun dishesSaved(redisEvent: RedisEvent) {
        val data = redisEvent.parseDb()["menu-items-inserted"] ?: throw InternalServerException("Insert failed")
        val state = redisEvent.parseState<MenuInputDto>()

        if (data.size < (state?.meals?.map { it.dishes }?.flatten()?.size ?: 0)) {
            throw InternalServerException("Insert failed")
        } else {
            redisEvent.switchOnApi(Response(
                success = true,
                data = "Menu saved"
            ))
        }
    }


    suspend fun selectMenu(redisEvent: RedisEvent) {
        val transaction = newAutoCommitTransaction {
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

        redisEvent.switchOnDb(transaction, redisEvent.mutate(GetUserMenuEventState.SELECTED))
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

    suspend fun menuSelected(redisEvent: RedisEvent) {
        val menu = redisEvent.parseDb()["select-menu"] ?: throw InternalServerException("Select failed")
        val menuItems = redisEvent.parseDb()["select-menu-items"] ?: throw InternalServerException("Select failed")

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