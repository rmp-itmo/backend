package com.rmp.loader.routines

import com.rmp.loader.core.ApiClient
import com.rmp.loader.core.Routine
import com.rmp.loader.dto.menu.CheckMenuItemDto
import com.rmp.loader.dto.menu.MenuDto
import io.ktor.client.call.*
import io.ktor.client.request.*

object MarkMenuItemDone {
    val routine = Routine {
        extend(MenuRoutine.routine)

        addStep("users/log/menu", ApiClient.Method.POST) {
            setBuilder { bot ->
                val menu = bot.state as MenuDto
                setBody(CheckMenuItemDto(
                    check = true,
                    menuItemId = menu.meals.first().dishes.first().menuItemId
                ))
            }
        }

        addStep("users/menu", ApiClient.Method.GET) {
            setProcessor { response ->
                state = response.body<MenuDto>()
            }
        }
    }
}