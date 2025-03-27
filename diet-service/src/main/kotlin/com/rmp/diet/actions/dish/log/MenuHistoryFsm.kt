package com.rmp.diet.actions.dish.log

import com.rmp.diet.services.DietLogService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class MenuHistoryFsm(di: DI) : Fsm("menu-history", di) {
    private val dietLogService: DietLogService by instance()

    override fun Fsm.registerStates() {
        on(MenuHistoryEventState.INIT) {
            dietLogService.getMenuHistory(this)
        }
    }
}