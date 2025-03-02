package com.rmp.lib.utils.korm.column

import com.rmp.lib.utils.korm.Table

class EntityId(table: Table, name: String) : Column<Long>(table, EntityIdColumn(), name)