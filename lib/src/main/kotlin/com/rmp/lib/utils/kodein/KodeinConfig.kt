package com.rmp.lib.utils.kodein

import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton

inline fun <reified T : Any> DI.MainBuilder.bindSingleton(crossinline callback: (DI) -> T) {
    bind<T>() with singleton { callback(this@singleton.di) }
}