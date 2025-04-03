package com.rmp.loader

import com.rmp.loader.core.Pool
import com.rmp.loader.routines.HelloRoutine
import kotlinx.coroutines.runBlocking

fun main() {
    val pool = Pool {
        append(HelloRoutine.routine, 500)
    }

    runBlocking {
        pool.run("Hello pool")
    }
}