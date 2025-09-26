package top.fifthlight.blazerod

import kotlinx.coroutines.CoroutineDispatcher

object BlazeRod {
    const val INSTANCE_SIZE = 128
    const val MAX_ENABLED_MORPH_TARGETS = 32
    const val COMPUTE_LOCAL_SIZE = 64

    lateinit var mainDispatcher: CoroutineDispatcher
    var debug = false
}