package com.rmp.user.actions.training.get

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.user.services.TrainingService
import org.kodein.di.DI
import org.kodein.di.instance

class TrainingIntensiveGetFsm(di: DI): Fsm("user-get-trainings-intensive", di) {
    private val trainingService: TrainingService by instance()

    enum class TrainingIntensiveGetEventState {
        INIT
    }

    override fun Fsm.registerStates() {
        on(TrainingIntensiveGetEventState.INIT) {
            trainingService.getIntensity(this)
        }
    }
}