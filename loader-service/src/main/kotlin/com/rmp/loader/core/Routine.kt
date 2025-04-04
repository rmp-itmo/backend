package com.rmp.loader.core

import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.random.nextLong

class Routine private constructor() {
    companion object {
        operator fun invoke(builder: Routine.() -> Unit) = Routine().apply(builder)
    }

    fun HttpRequestBuilder.randomString(l: Int): String {
        val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..l)
            .map { charset.random() }
            .joinToString("")
    }

    fun HttpRequestBuilder.randomDouble(a: Double = Double.MIN_VALUE, b: Double = Double.MAX_VALUE) =
        Random.nextDouble(a, b)

    fun HttpRequestBuilder.randomInt(a: Int = Int.MIN_VALUE, b: Int = Int.MAX_VALUE) =
        Random.nextInt(a..b)

    fun HttpRequestBuilder.randomLong(a: Long = Long.MIN_VALUE, b: Long = Long.MAX_VALUE) =
        Random.nextLong(a..b)

    fun HttpRequestBuilder.randomFloat(a: Float = Float.MIN_VALUE, b: Float = Float.MAX_VALUE) =
        randomDouble(a.toDouble(), b.toDouble()).toFloat()

    fun HttpRequestBuilder.randomBoolean() =
        randomInt(1, 2) == 1


    class RoutineFinished(): Exception("Routine finished")

    class RoutineFailed(): Exception("Routine failed")

    sealed class Step private constructor() {
        class CallStep(val url: String, val method: ApiClient.Method, val authorized: Boolean): Step() {
            var builder: HttpRequestBuilder.(state: Pool.PoolItem) -> Unit = {}
            var processor: suspend Pool.PoolItem.(response: HttpResponse) -> Unit = {}

            companion object {
                operator fun invoke(url: String, method: ApiClient.Method, authorized: Boolean, builder: CallStep.() -> Unit): CallStep = CallStep(url, method, authorized).apply(builder)
            }

            @JvmName("setBuilderMethod")
            fun setBuilder(builder: HttpRequestBuilder.(state: Pool.PoolItem) -> Unit) {
                this.builder = builder
            }

            @JvmName("setProcessorMethod")
            fun setProcessor(processor: suspend Pool.PoolItem.(response: HttpResponse) -> Unit) {
                this.processor = processor
            }
        }
        class DelayStep(val timeout: Long): Step()
    }

    private val steps: MutableList<Step> = mutableListOf()

    val size: Int get() = steps.size

    fun addStep(url: String, method: ApiClient.Method, authorized: Boolean = true, callBuilder: Step.CallStep.() -> Unit) {
        steps += Step.CallStep(url, method, authorized, callBuilder)
    }

    fun addDelay(timeout: Long) {
        steps += Step.DelayStep(Random.nextLong(timeout - 50, timeout + 50))
    }

    fun addDelay(a: Long, b: Long) {
        steps += Step.DelayStep(Random.nextLong(a, b))
    }

    suspend fun runStepOn(poolItem: Pool.PoolItem): String {
        if (poolItem.curStep > steps.lastIndex) throw RoutineFinished()

        return when (val step = steps[poolItem.curStep]) {
            is Step.CallStep -> {
                val now = System.currentTimeMillis()
                println("Start execute Bot#${poolItem.id} step ${step.url}")
                val builder: HttpRequestBuilder.() -> Unit = {
                    step.builder(this, poolItem)
                }

                val response = if (step.authorized)
                    poolItem.apiClient.authorizedRequest(step.method, step.url, builder)
                else
                    poolItem.apiClient.unauthorizedRequest(step.method, step.url, builder)

                with(poolItem) {
                    step.processor(this, response.successOr(null) ?: throw RoutineFailed())
                }
                println("Execution Bot#${poolItem.id} step ${step.url} succeed, executed ${System.currentTimeMillis() - now} ms")
                "success"
            }
            is Step.DelayStep -> {
                println("Start execute Bot#${poolItem.id} delay ${step.timeout}ms")
                delay(step.timeout)
                "delayed"
            }
        }
    }
}