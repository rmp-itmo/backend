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

fun main(args: Array<String>) {
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

    val testPool = Pool {
        append(WaterRoutine.routine, 1)
    }

    runBlocking {
        pool.run("Test pool")
    }
}