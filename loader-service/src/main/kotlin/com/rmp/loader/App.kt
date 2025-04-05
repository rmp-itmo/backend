package com.rmp.loader

import com.rmp.loader.core.Pool
import com.rmp.loader.dto.FetchGraphDto
import com.rmp.loader.routines.*
import kotlinx.coroutines.runBlocking

const val TODAY = 20250404

const val CURRENT_MONTH = TODAY / 100

val TODAY_GRAPH = FetchGraphDto(
    year = 2025,
    month = "04",
    day = "04"
)

val CURRENT_MONTH_GRAPH = FetchGraphDto(
    year = 2025,
    month = "04"
)

val CURRENT_YEAR_GRAPH = FetchGraphDto(
    year = 2025
)

fun main() {
    val pool = Pool {
        append(HelloRoutine.routine, 3000)
    }
    val testPool = Pool {
        append(HelloRoutine.routine, 1)
        append(HeartRoutine.routine, 1)
        append(MenuHistoryRoutine.routine, 1)
        append(MenuRoutine.routine, 1)
        append(SleepRoutine.routine, 1)
        append(WaterRoutine.routine, 1)
        append(MarkMenuItemDone.routine, 1)
        append(MainPageRoutine.routine, 1)
        append(AchievementRoutine.routine, 1)
        append(FeedRoutine.routine, 1)
        append(LikeRoutine.routine, 1)
        append(TrainingRoutine.routine, 1)
        append(SettingsRoutine.routine, 1)
        append(SubscribeRoutine.routine, 1)
    }

    runBlocking {
//        pool.run("Hello pool")
        testPool.run("Test pool")
    }
}