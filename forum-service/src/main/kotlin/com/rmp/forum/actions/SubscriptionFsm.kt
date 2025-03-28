package com.rmp.forum.actions

import com.rmp.forum.services.SubscribeService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class SubscriptionFsm(di: DI) : Fsm("subscription", di) {
    private val subscribeService: SubscribeService by instance()

    enum class SubscriptionEventState {
        INIT
    }

    override fun Fsm.registerStates() {
        on(SubscriptionEventState.INIT) {
            subscribeService.manageSubscription(this)
        }
    }
}