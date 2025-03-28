package com.rmp.user.actions.training.get

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.user.services.TrainingService
import org.kodein.di.DI
import org.kodein.di.instance

class TrainingsGetFsm(di: DI): Fsm("user-get-trainings", di) {
    private val trainingService: TrainingService by instance()

    enum class TrainingsGetEventState {
        INIT
    }

    override fun Fsm.registerStates() {
        on(TrainingsGetEventState.INIT) {
            trainingService.getTrainings(this)
        }
    }
}