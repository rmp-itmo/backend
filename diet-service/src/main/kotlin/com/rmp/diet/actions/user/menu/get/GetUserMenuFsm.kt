package com.rmp.diet.actions.user.menu.get

import com.rmp.diet.services.MenuService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class GetUserMenuFsm(di: DI) : Fsm("get-user-menu", di) {
    private val menuService: MenuService by instance()

    override fun Fsm.registerStates() {
        on(GetUserMenuEventState.INIT) {
            menuService.selectMenu(this)
        }
    }

}