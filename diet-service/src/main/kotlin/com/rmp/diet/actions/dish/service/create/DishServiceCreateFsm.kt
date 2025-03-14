package com.rmp.diet.actions.dish.service.create

import org.kodein.di.instance
import com.rmp.diet.services.DishService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI

class DishServiceCreateFsm(di: DI): Fsm("service-dish-upload", di) {
    private val dishService: DishService by instance()
    override fun Fsm.registerStates() {
        on(DishServiceCreateEventState.INIT) {
            dishService.createDish(this)
        }

        on(DishServiceCreateEventState.CREATED) {
            dishService.dishCreated(this)
        }
    }
}