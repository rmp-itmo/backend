package com.rmp.paprika.actions.meal

enum class GenerateMealState {
    INIT,

    CACHE_FETCHED,

    SOLVED_BY_CACHE,

    SEARCH_DISHES,

    DISHES_FETCHED,
}