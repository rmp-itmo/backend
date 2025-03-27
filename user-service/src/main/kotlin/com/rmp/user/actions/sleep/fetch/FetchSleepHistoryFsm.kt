package com.rmp.user.actions.sleep.fetch

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.user.services.SleepService
import org.kodein.di.DI
import org.kodein.di.instance

class FetchSleepHistoryFsm(di: DI) : Fsm("fetch-sleep-history", di) {
    private val sleepService: SleepService by instance()

    override fun Fsm.registerStates() {
        on(FetchSleepHistoryEventState.INIT) {
            sleepService.getSleepHistory(this)
        }
    }
}