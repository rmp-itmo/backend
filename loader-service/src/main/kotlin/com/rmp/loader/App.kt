package com.rmp.loader

import com.rmp.loader.core.Pool
import com.rmp.loader.routines.HelloRoutine
import kotlinx.coroutines.runBlocking

fun main() {
    val pool = Pool {
        append(HelloRoutine.routine, 3000)
    }
    val testPool = Pool {
        append(HelloRoutine.routine, 1)
    }

    runBlocking {
//        pool.run("Hello pool")
        testPool.run("Test pool")
    }
}