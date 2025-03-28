package com.rmp.diet.actions.dish.fetch

import com.rmp.diet.services.DishService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class DishFetchFsm(di: DI) : Fsm("dish-fetch", di) {
    private val dishService: DishService by instance()

    override fun Fsm.registerStates() {
        on(DishFetchEventState.INIT) {
            dishService.fetchDish(this)
        }
    }
}