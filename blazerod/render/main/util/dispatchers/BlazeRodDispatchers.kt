package top.fifthlight.blazerod.util.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import top.fifthlight.blazerod.BlazeRod

object BlazeRodDispatchers {
    val Main: CoroutineDispatcher
        get() = BlazeRod.mainDispatcher
}

val Dispatchers.BlazeRod
    get() = BlazeRodDispatchers
