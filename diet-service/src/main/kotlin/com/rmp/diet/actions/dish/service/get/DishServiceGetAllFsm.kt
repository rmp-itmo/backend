package com.rmp.diet.actions.dish.service.get

import org.kodein.di.instance
import com.rmp.diet.services.DishService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI

class DishServiceGetAllFsm(di: DI): Fsm("service-dish-get-all", di) {
    private val dishService: DishService by instance()
    override fun Fsm.registerStates() {
        on(DishServiceGetAllEventState.INIT) {
            dishService.getDishes(this)
        }

        on(DishServiceGetAllEventState.RESPONSE) {
            dishService.getResponse(this)
        }
    }
}