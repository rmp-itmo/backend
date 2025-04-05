package com.rmp.loader.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import kotlin.random.nextLong

class Pool private constructor() {
    private val items: MutableList<PoolItem> = mutableListOf()

    class PoolItem(val id: Int, val routine: Routine) {
        var curStep = 0
        val apiClient = ApiClient()
        var state: Any? = null

        fun authorize(tokenDto: TokenDto) {
            apiClient.authorizationData = tokenDto
        }

        suspend fun run() {
            println("------- Bot#$id started ${System.currentTimeMillis()} --------")
            for (i in 0..<routine.size) {
                try {
                    routine.runStepOn(this)
                } catch (e: Exception) {
                    println("Exception during step #$curStep in thread Bot#$id")
                    println(e.message)
                    println(e.stackTraceToString())
                    throw e
                }
                curStep += 1
            }
        }
    }

    companion object { operator fun invoke(poolBuilder: Pool.() -> Unit): Pool = Pool().apply(poolBuilder) }

    fun append(routine: Routine, count: Int) {
        items.addAll(List(count) { PoolItem(items.size + it, routine) })
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
                        it.run()
                        println("Bot#${it.id} finished successfully")
                    } catch (e: Exception) {
                        println("Bot#${it.id} failed")
                    }
                }
//                delay(Random.nextLong(200..250L))
                delay(Random.nextLong(25..50L))
            }
        }
        println("$poolName execution time: ${System.currentTimeMillis() - now} ms")
    }
}