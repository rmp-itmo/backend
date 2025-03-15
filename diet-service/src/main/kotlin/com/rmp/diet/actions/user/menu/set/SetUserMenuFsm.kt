package com.rmp.diet.actions.user.menu.set

import com.rmp.diet.services.MenuService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class SetUserMenuFsm(di: DI) : Fsm("set-user-menu", di) {
    private val menuService: MenuService by instance()

    override fun Fsm.registerStates() {
        on(SetUserMenuEventState.INIT) {
            menuService.setMenu(this)
        }
    }

}