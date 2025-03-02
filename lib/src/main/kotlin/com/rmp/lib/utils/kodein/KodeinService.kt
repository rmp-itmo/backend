package com.rmp.lib.utils.kodein

import org.kodein.di.DI
import org.kodein.di.DIAware

abstract class KodeinService(override val di: DI): DIAware