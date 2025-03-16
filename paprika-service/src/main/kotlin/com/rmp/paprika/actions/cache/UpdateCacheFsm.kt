package com.rmp.paprika.actions.cache

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.paprika.services.CacheService
import org.kodein.di.DI
import org.kodein.di.instance

class UpdateCacheFsm(di: DI) : Fsm("update-cache", di) {
    private val cacheService: CacheService by instance()

    override fun Fsm.registerStates() {
        on(UpdateCacheState.SAVE_CACHE) {
            cacheService.saveCache(this)
        }

        on(UpdateCacheState.SAVED) {}
    }
}