package com.rmp.loader.routines

import com.rmp.loader.TODAY
import com.rmp.loader.core.ApiClient
import com.rmp.loader.core.Routine
import com.rmp.loader.dto.FetchHistoryDto
import com.rmp.loader.dto.menu.*
import io.ktor.client.call.*
import io.ktor.client.request.*

object MenuRoutine {
    val routine = Routine {
        startBot()

        addStep("users/stat/summary", ApiClient.Method.POST) {
            setBuilder {
                setBody(FetchHistoryDto(date = TODAY))
            }
        }

        addDelay(100) // Delay between 50 and 150

        addStep("paprika/calculate", ApiClient.Method.POST) {
            setBuilder {
                val paprikaInput = PaprikaInputDto(
                    calories = randomInt(2000, 2600),
                    meals = List(3) {
                        PaprikaInputDto.Meal("Meal$it", randomDouble(0.1, 1.0), it + 1)
                    }
                )

                setBody(paprikaInput)
            }

            setProcessor { response ->
                state = response.body<PaprikaGenerateResult>()
            }
        }

        addDelay(100)

        addStep("users/menu", ApiClient.Method.POST) {
            setBuilder { bot ->
                val paprikaGenerateResult = bot.state as PaprikaGenerateResult
                val setMenuDto = SetMenuDto(
                    meals = paprikaGenerateResult.meals.map {
                        SetMenuDto.Meal(dishes = it.dishes.map { dish -> dish.id }, name = it.name)
                    },
                    params = paprikaGenerateResult.params
                )
                setBody(setMenuDto)
            }
        }

        addDelay(100)

        addStep("users/menu", ApiClient.Method.GET) {
            setProcessor { response ->
                state = response.body<MenuDto>()
            }
        }

        addDelay(100)

        addStep("users/menu", ApiClient.Method.PATCH) {
            setBuilder { bot ->
                val menu = bot.state as MenuDto
                val addMenuItemDto = AddMenuItemDto(
                    check = false,
                    dishId = randomInt(10, 100),
                    mealId = menu.meals.first().mealId
                )

                setBody(addMenuItemDto)
            }
        }

        addDelay(100)

        addStep("users/menu", ApiClient.Method.GET) {
            setProcessor { response ->
                state = response.body<MenuDto>()
            }
        }
    }
}