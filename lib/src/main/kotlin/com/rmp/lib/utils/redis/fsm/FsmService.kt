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

abstract class FsmService(di: DI) : KodeinService(di) {
    val pubSubService: PubSubService by instance()

    suspend inline fun <reified T: Query> RedisEvent.switchOnDb(
        queryDto: T, state: RedisEventState = eventState
    ) = pubSubService.publish(mutateData(queryDto).mutate(state), AppConf.redis.db)

    suspend inline fun <reified T: SerializableClass> RedisEvent.switchOnApi(
        responseDto: T
    ) = pubSubService.publish(mutateData(responseDto).mutate(mutate(RedisEventState.State.TERMINAL)), AppConf.redis.api)

    suspend inline fun RedisEvent.switchOn(
        service: String
    ) = pubSubService.publish(this, service)

    suspend inline fun <reified T: SerializableClass> RedisEvent.switchOn(
        data: T, service: String, state: RedisEventState = eventState
    ) = pubSubService.publish(mutateData(data).mutate(state), service)

    suspend inline fun RedisEvent.switchOn(
        service: String, state: RedisEventState = eventState
    ) = pubSubService.publish(mutate(state), service)

    suspend fun RedisEvent.switch(
        service: String, state: RedisEventState = eventState
    ) = pubSubService.publish(mutate(state), service)
}