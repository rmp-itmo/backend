package com.rmp.lib.utils.redis.fsm

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.utils.kodein.KodeinService
import com.rmp.lib.utils.korm.query.Query
import com.rmp.lib.utils.redis.PubSubService
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.RedisEventState
import com.rmp.lib.utils.redis.SerializableClass
import org.kodein.di.DI
import org.kodein.di.instance

abstract class FsmService(di: DI, val channel: String = "service") : KodeinService(di) {
    val pubSubService: PubSubService by instance()
    val fsmRouter: FsmRouter by instance()

    suspend inline fun <reified T: Query> RedisEvent.switchOnDb(
        queryDto: T, state: RedisEventState = eventState
    ) = pubSubService.publish(mutateData(queryDto).mutate(state), AppConf.redis.db)

    suspend inline fun <reified T: SerializableClass> RedisEvent.switchOnApi(
        responseDto: T
    ) = pubSubService.publish(mutateData(responseDto).mutate(mutate(RedisEventState.State.TERMINAL)), AppConf.redis.api)

    suspend inline fun RedisEvent.switchOn(
        service: String
    ) {
        if (service == channel)
            fsmRouter.process(this)
        else
            pubSubService.publish(this, service)
    }

    suspend inline fun <reified T: SerializableClass> RedisEvent.switchOn(
        data: T, service: String, state: RedisEventState = eventState
    ) {
        val updated = mutateData(data).mutate(state)
        if (service == channel)
            fsmRouter.process(updated)
        else
            pubSubService.publish(updated, service)
    }

    suspend fun RedisEvent.switch(
        service: String, state: RedisEventState = eventState
    ) = pubSubService.publish(mutate(state), service)
}