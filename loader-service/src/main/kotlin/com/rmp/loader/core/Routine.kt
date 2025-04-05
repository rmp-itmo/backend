package com.rmp.loader.core

import com.rmp.loader.dto.hello.LoginDto
import com.rmp.loader.dto.hello.SignupDto
import io.ktor.client.call.*
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

    class RoutineFailed(bot: String, request: String): Exception("Routine failed $bot, $request")

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

    fun authorize(loginDto: LoginDto) {
        addStep("auth", ApiClient.Method.POST, false) {
            setBuilder { bot ->
                setBody(loginDto)
            }
            setProcessor { response ->
                authorize(response.body())
            }
        }
    }

    fun authorize(getLoginDto: Pool.PoolItem.() -> LoginDto) {

        addStep("auth", ApiClient.Method.POST, false) {
            setBuilder { bot ->
                setBody(with(bot, getLoginDto))
            }
            setProcessor { response ->
                authorize(response.body())
            }
        }
    }

    fun startBot() {
        addStep("users/create", ApiClient.Method.POST, false) {
            setBuilder { bot ->
                val login = LoginDto("${randomString(6)}-${System.currentTimeMillis()}", "password")
                println("Bot data: $login")
                bot.state = login
                val signupDto = SignupDto(
                    name = "test-${randomString(6)}-${System.currentTimeMillis()}",
                    email = login.login,
                    password = login.password,
                    height = randomFloat(1f, 2f),
                    weight = randomFloat(45f, 150f),
                    activityType = randomLong(1, 3),
                    goalType = randomLong(1, 3),
                    isMale = randomBoolean(),
                    age = randomInt(18, 70),
                    registrationDate = randomInt(1, 100)
                )
                setBody(signupDto)
            }
        }

        addDelay(100)

        authorize {
            state as LoginDto
        }
    }

    fun extend(routine: Routine) {
        steps += routine.steps
    }

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
                println("Start execute Bot#${poolItem.id} step ${step.method} ${step.url}")
                val builder: HttpRequestBuilder.() -> Unit = {
                    step.builder(this, poolItem)
                }

                val response = if (step.authorized)
                    poolItem.apiClient.authorizedRequest(step.method, step.url, builder)
                else
                    poolItem.apiClient.unauthorizedRequest(step.method, step.url, builder)

                with(poolItem) {
                    try {
                        val httpResponse = response.successOr(null) ?: throw Exception("Bot#${this.id} ${step.method} ${step.url}")
                        if (httpResponse.status.value != 200) throw Exception("${httpResponse.status.value}")
                        step.processor(this, httpResponse)
                    } catch (e: Exception) {
                        println(response.successOr(null)?.bodyAsText())
                        println(e.message)
                        println(e.stackTraceToString())
                        throw RoutineFailed("Bot#${this.id}", "${step.method} ${step.url}")
                    }
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