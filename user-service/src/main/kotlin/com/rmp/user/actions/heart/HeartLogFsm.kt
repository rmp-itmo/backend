package com.rmp.user.actions.heart

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.user.services.HeartService
import org.kodein.di.DI
import org.kodein.di.instance

class HeartLogFsm(di: DI): Fsm("user-upload-heart-log", di) {
    private val heartService: HeartService by instance()

    override fun Fsm.registerStates() {
        on(HeartLogEventState.INIT) {
            heartService.heartLog(this)
        }
    }
}