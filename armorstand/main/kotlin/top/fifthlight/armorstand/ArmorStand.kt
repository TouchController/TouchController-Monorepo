package top.fifthlight.armorstand

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

interface ArmorStand {
    val scope: CoroutineScope
    val mainDispatcher: CoroutineDispatcher

    companion object {
        lateinit var instance: ArmorStand
    }
}
