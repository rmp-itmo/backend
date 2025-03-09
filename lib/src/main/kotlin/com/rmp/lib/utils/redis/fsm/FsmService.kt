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
    ) = pubSubService.publish(switchData(queryDto).switch(state), AppConf.redis.db)

    suspend inline fun <reified T: SerializableClass> RedisEvent.switchOnApi(
        responseDto: T
    ) = pubSubService.publish(switchData(responseDto).switch(mutateState(RedisEventState.State.TERMINAL)), AppConf.redis.api)

    suspend inline fun <reified T: SerializableClass> RedisEvent.switchOn(
        data: T, service: String, state: RedisEventState = eventState
    ) = pubSubService.publish(switchData(data).switch(state), service)

    suspend inline fun <reified T: SerializableClass> RedisEvent.switchOn(
        service: String, state: RedisEventState = eventState
    ) = pubSubService.publish(switch(state), service)
}