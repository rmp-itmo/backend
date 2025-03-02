package com.rmp.lib.utils.korm.references

enum class ReferenceOption(val sql: String) {
    CASCADE("cascade"), SET_NULL("set null"), RESTRICT("restrict")
}