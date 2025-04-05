package com.rmp.loader

import com.rmp.loader.core.Pool
import com.rmp.loader.dto.FetchGraphDto
import com.rmp.loader.routines.*
import kotlinx.coroutines.runBlocking

const val TODAY = 20250405

const val CURRENT_MONTH = TODAY / 100

val TODAY_GRAPH = FetchGraphDto(
    year = 2025,
    month = "04",
    day = "05"
)

val CURRENT_MONTH_GRAPH = FetchGraphDto(
    year = 2025,
    month = "04"
)

val CURRENT_YEAR_GRAPH = FetchGraphDto(
    year = 2025
)

var startDelayMin = 20
var startDelayMax = 50L

var botNameSuffix = ""

fun main(args: Array<String>) {
    if (args.size <= 0) {
        println("Usage: \n Generate users: loader.jar generate <INT - Amount> <String - NameSuffix> <DelayMin - Int> <DelayMax - Int>")
        println("Usage: \n Run test: loader.jar 1 <INT - Amount> <String - NameSuffix> <DelayMin - Int> <DelayMax - Int>")
        return
    }

    val size = if (args.size > 1) {
        args[1].toInt()
    } else 100


    val pool = Pool {
        append(
            size,
            HelloRoutine.routine, HeartRoutine.routine, MenuHistoryRoutine.routine,
            MenuRoutine.routine, SleepRoutine.routine, WaterRoutine.routine,
            MarkMenuItemDone.routine, MainPageRoutine.routine, AchievementRoutine.routine,
            FeedRoutine.routine, LikeRoutine.routine, TrainingRoutine.routine,
            SettingsRoutine.routine, SubscribeRoutine.routine
        )
    }

    val generator = Pool {
        append(GenerateBot.routine, size)
    }

    if (args.size > 2) {
        botNameSuffix = args[2]
        println("select count(*) from user_model where email like 'bot#%_${botNameSuffix}'")
    }
    if (args.size > 3) {
        startDelayMin = args[3].toInt()
        startDelayMax = args[4].toLong()
    }

    runBlocking {
        if (args[0] == "generate") {
            generator.run("Generator")
        } else {
            pool.run("Test pool")
        }
    }
}