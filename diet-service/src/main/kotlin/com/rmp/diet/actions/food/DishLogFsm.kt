package com.rmp.diet.actions.food

import com.rmp.diet.services.DietLogService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class DishLogFsm(event: String, di: DI) : Fsm(event, di) {
    private val dietLogService: DietLogService by instance()

    override fun Fsm.registerStates() {
        on(DishLogEventState.INIT) {
            dietLogService.uploadDish(this)
        }

        on(DishLogEventState.CREATE_DISH) {
            dietLogService.createDish(this)
        }

        on(DishLogEventState.CREATED) {
            dietLogService.dishCreated(this)
        }
    }
}