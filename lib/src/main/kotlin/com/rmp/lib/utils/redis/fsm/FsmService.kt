package com.rmp.lib.utils.redis.fsm

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.utils.kodein.KodeinService
import com.rmp.lib.utils.korm.query.Query
import com.rmp.lib.utils.korm.query.batch.BatchBuilder
import com.rmp.lib.utils.redis.*
import org.kodein.di.DI
import org.kodein.di.instance

abstract class FsmService(di: DI, val channel: String = "service") : KodeinService(di) {
    val pubSubService: PubSubService by instance()
    val fsmRouter: FsmRouter by instance()

    private suspend inline fun <reified T: Query> RedisEvent.dbRequest(
        queryDto: T
    ): DbResponseData {
        val id = System.nanoTime()
        pubSubService.publish(forDb(id).mutateData(queryDto), AppConf.redis.db)
        return pubSubService.regDbRequest(id, this).await()
    }

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

    suspend fun transaction(redisEvent: RedisEvent, builder: BatchBuilder.() -> Unit): DbResponseData =
        BatchBuilder().apply(builder).batch.let {
            redisEvent.dbRequest(it)
        }

    suspend fun newTransaction(redisEvent: RedisEvent, builder: BatchBuilder.() -> Unit): DbResponseData =
        BatchBuilder().apply {
            init()
        }.apply(builder).batch.let {
            redisEvent.dbRequest(it)
        }

    suspend fun autoCommitTransaction(redisEvent: RedisEvent, builder: BatchBuilder.() -> Unit): DbResponseData =
        BatchBuilder()
            .apply(builder)
            .apply {
                commit()
            }.batch.let {
                redisEvent.dbRequest(it)
            }

    suspend fun newAutoCommitTransaction(redisEvent: RedisEvent, builder: BatchBuilder.() -> Unit): DbResponseData =
        BatchBuilder()
            .apply {
                init()
            }
            .apply(builder)
            .apply {
                commit()
            }.batch.let {
                redisEvent.dbRequest(it)
            }
}