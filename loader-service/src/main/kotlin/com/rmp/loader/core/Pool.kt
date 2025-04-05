package com.rmp.loader.core

import com.rmp.loader.startDelayMax
import com.rmp.loader.startDelayMin
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.random.nextLong

class Pool private constructor() {
    private val items: MutableList<PoolItem> = mutableListOf()
    private val failures: MutableMap<String, Int> = mutableMapOf()
    private var totalSteps: Long = 0L

    @OptIn(ObsoleteCoroutinesApi::class)
    val failuresActor = CoroutineScope(Dispatchers.Default).actor<Pair<String, PoolItem>> {
        for (item in this) {
            val (_, poolItem) = item
            val url = poolItem.routine.getStepUrl(poolItem.curStep) ?: continue
            if (url in failures) {
                failures[url] = failures[url]!! + 1
            } else {
                failures += url to 1
            }
        }
    }

    class PoolItem(val id: Int, val routine: Routine) {
        var curStep = 0
        val apiClient = ApiClient()
        var state: Any? = null

        fun authorize(tokenDto: TokenDto) {
            apiClient.authorizationData = tokenDto
        }

        suspend fun run(pool: Pool) {
            println("------- Bot#$id started ${System.currentTimeMillis()} --------")
            for (i in 0..<routine.size) {
                try {
                    routine.runStepOn(this)
                    curStep += 1
                } catch (e: Exception) {
                    println("Exception during step #$curStep in thread Bot#$id")
                    if (e.message?.split(" ")?.first() == "Connect") {
                        pool.failuresActor.send("connect" to this)
                    } else {
                        pool.failuresActor.send("unknown" to this)
                    }
                    println(e.message)
                    println(e.stackTraceToString())
                    throw e
                }
            }
        }
    }

    companion object { operator fun invoke(poolBuilder: Pool.() -> Unit): Pool = Pool().apply(poolBuilder) }

    fun append(routine: Routine, count: Int) {
        items.addAll(List(count) { PoolItem(items.size + it, routine) })
        totalSteps += routine.sizeWithoutDelay * count
    }

    fun append(count: Int, vararg routines: Routine) {
        val part = count / routines.size
        for (route in routines) {
            append(route, part)
        }
        items.shuffle()
    }

    suspend fun run(poolName: String) {
        val now = System.currentTimeMillis()
        println("Start $poolName (${items.size}) execution")
        withContext(Dispatchers.IO) {
            items.forEach {
                launch {
                    try {
                        it.run(this@Pool)
                    } catch (e: Exception) {
                        println("Bot#${it.id} failed")
                    }
                }
//                delay(Random.nextLong(200..250L))
                delay(Random.nextLong(startDelayMin..startDelayMax))
            }
        }
        println("$poolName execution time: ${System.currentTimeMillis() - now} ms")
        println("Total steps: $totalSteps")
        val totalFailures = failures.map { it.value }.sum().toDouble()
        println("Execution failures: (${totalFailures} - ${(totalFailures / totalSteps.toDouble()) * 100}%)")
        println(Json { prettyPrint = true }.encodeToString(failures))
    }
}