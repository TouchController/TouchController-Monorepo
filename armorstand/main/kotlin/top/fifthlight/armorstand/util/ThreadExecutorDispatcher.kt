package top.fifthlight.armorstand.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import net.minecraft.util.thread.ThreadExecutor
import kotlin.coroutines.CoroutineContext

class ThreadExecutorDispatcher(private val executor: ThreadExecutor<*>) : CoroutineDispatcher() {
    override fun isDispatchNeeded(context: CoroutineContext) = !executor.isOnThread

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (executor.isOnThread) {
            Dispatchers.Unconfined.dispatch(context, block)
        } else {
            executor.execute(block)
        }
    }
}