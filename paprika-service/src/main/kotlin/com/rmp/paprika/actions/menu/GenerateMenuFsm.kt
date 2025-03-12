package com.rmp.paprika.actions.menu

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.paprika.services.PaprikaService
import org.kodein.di.DI
import org.kodein.di.instance

class GenerateMenuFsm(di: DI) : Fsm("generate-menu", di) {
    private val paprikaService: PaprikaService by instance()

    override fun Fsm.registerStates() {
        on(GenerateMenuState.INIT) {
            paprikaService.init(this)
        }

        on(GenerateMenuState.GENERATE_MEAL) {
            paprikaService.generateMeal(this)
        }

        on(GenerateMenuState.MEAL_GENERATED) {
            paprikaService.mealGenerated(this)
        }
    }
}