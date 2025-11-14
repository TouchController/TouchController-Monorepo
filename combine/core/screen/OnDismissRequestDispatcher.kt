package top.fifthlight.combine.screen

import androidx.compose.runtime.staticCompositionLocalOf

interface OnDismissHandler {
    val isEnabled: Boolean
    fun handleOnDismissed()
}

interface OnDismissRequestDispatcher {
    fun addHandler(handler: OnDismissHandler)
    fun removeHandler(handler: OnDismissHandler)
    fun dispatchOnDismissed()
    fun hasEnabledCallbacks(): Boolean

    companion object Empty : OnDismissRequestDispatcher {
        override fun addHandler(handler: OnDismissHandler) {}
        override fun removeHandler(handler: OnDismissHandler) {}
        override fun dispatchOnDismissed() {}
        override fun hasEnabledCallbacks() = false
    }
}

fun OnDismissRequestDispatcher(): OnDismissRequestDispatcher = OnDismissRequestDispatcherImpl()

val LocalOnDismissRequestDispatcher =
    staticCompositionLocalOf<OnDismissRequestDispatcher> { OnDismissRequestDispatcher.Empty }

private class OnDismissRequestDispatcherImpl : OnDismissRequestDispatcher {
    private val handlers = arrayListOf<OnDismissHandler>()

    override fun addHandler(handler: OnDismissHandler) {
        handlers.add(handler)
    }

    override fun removeHandler(handler: OnDismissHandler) {
        handlers.remove(handler)
    }

    override fun dispatchOnDismissed() {
        handlers.lastOrNull { it.isEnabled }?.handleOnDismissed()
    }

    override fun hasEnabledCallbacks(): Boolean = handlers.any { it.isEnabled }
}
