package com.rmp.diet.actions.dish.log

import com.rmp.diet.services.DietLogService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class DishLogFsm(di: DI): Fsm("user-upload-dish-log", di) {
    private val dietLogService: DietLogService by instance()

    override fun Fsm.registerStates() {
        on(DishLogEventState.INIT) {
            dietLogService.uploadDish(this)
        }

        on(DishLogEventState.LOG_NEW) {
            dietLogService.logNew(this)
        }
    }
}