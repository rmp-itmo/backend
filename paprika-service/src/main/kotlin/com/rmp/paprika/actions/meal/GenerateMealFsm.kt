package com.rmp.paprika.actions.meal

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.paprika.services.PaprikaService
import org.kodein.di.DI
import org.kodein.di.instance

class GenerateMealFsm(di: DI) : Fsm("generate-meal", di) {
    private val paprikaService: PaprikaService by instance()

    override fun Fsm.registerStates() {
        on(GenerateMealState.INIT) {
            paprikaService.initMealSolution(this)
        }

        on(GenerateMealState.SEARCH_DISHES) {
            paprikaService.searchDishes(this)
        }
    }
}