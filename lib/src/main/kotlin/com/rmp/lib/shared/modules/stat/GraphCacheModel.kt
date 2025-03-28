package com.rmp.lib.shared.modules.stat

import com.rmp.lib.utils.korm.IdTable

object GraphCacheModel: IdTable("graph_cache") {
    val name = text("graph_name")
    val top = int("top_bound")
    val bottom = int("bottom_bound")
    val results = text("results")
}