package com.rmp.user.actions.summary

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.user.services.SleepService
import com.rmp.user.services.UserService
import org.kodein.di.DI
import org.kodein.di.instance

class UserSummaryFsm(di: DI): Fsm("user-summary", di) {
    private val userService: UserService by instance()
    private val sleepService: SleepService by instance()

    override fun Fsm.registerStates() {
        on(UserSummaryEventState.INIT) {
            sleepService.getSleepTimePerDay(this)
        }
        on(UserSummaryEventState.SUMMARIZE) {
           userService.getUserSummary(this)
        }
    }
}