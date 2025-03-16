package com.rmp.lib.utils.korm.column

import com.rmp.lib.utils.korm.Table

class EntityCount(table: Table, name: String) : Column<Long>(table, LongColumn(), name)