package com.rmp.diet.actions.user.menu.set

import com.rmp.diet.services.MenuService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class AddMenuItemFsm(di: DI) : Fsm("user-update-menu", di) {
    private val menuService: MenuService by instance()

    override fun Fsm.registerStates() {
        on(AddMenuItemEventState.INIT) {
            menuService.addMenuItem(this)
        }
    }
}