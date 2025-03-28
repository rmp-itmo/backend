package com.rmp.diet.actions.user.menu.set

import com.rmp.diet.services.MenuService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class RemoveMenuItemFsm(di: DI) : Fsm("remove-menu-item", di) {
    private val menuService: MenuService by instance()

    override fun Fsm.registerStates() {
        on(RemoveMenuItemEventState.INIT) {
            menuService.removeMenuItem(this)
        }
    }
}