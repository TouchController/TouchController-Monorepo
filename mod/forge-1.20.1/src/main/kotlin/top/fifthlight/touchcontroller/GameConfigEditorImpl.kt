package top.fifthlight.touchcontroller

import net.minecraft.client.Minecraft
import net.minecraft.client.OptionInstance
import net.minecraft.client.Options
import top.fifthlight.touchcontroller.config.GameConfigEditor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.reflect.KProperty

object GameConfigEditorImpl : GameConfigEditor {
    private val pendingCallbackLock = ReentrantLock()
    private var pendingCallbacks: MutableList<GameConfigEditor.Callback>? = mutableListOf()

    operator fun <T> OptionInstance<T>.getValue(thisRef: Any?, property: KProperty<*>): T = this.get()
    operator fun <T> OptionInstance<T>.setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (value != null) {
            this.set(value)
        }
    }

    private class EditorImpl(val options: Options) : GameConfigEditor.Editor {
        override var autoJump: Boolean by options.autoJump()
    }

    fun executePendingCallback() {
        pendingCallbackLock.withLock {
            val callbacks = pendingCallbacks
            if (callbacks == null) {
                return
            }
            pendingCallbacks = null
            with(EditorImpl(Minecraft.getInstance().options)) {
                callbacks.forEach { callback ->
                    callback.invoke(this)
                }
                options.save()
            }
        }
    }

    override fun submit(callback: GameConfigEditor.Callback) {
        pendingCallbackLock.withLock {
            pendingCallbacks?.add(callback) ?: run {
                with(EditorImpl(Minecraft.getInstance().options)) {
                    callback.invoke(this)
                    options.save()
                }
            }
        }
    }
}
