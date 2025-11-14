package top.fifthlight.combine.animation

import androidx.compose.runtime.staticCompositionLocalOf
import aurelienribon.tweenengine.Tween
import aurelienribon.tweenengine.TweenCallback
import aurelienribon.tweenengine.TweenManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

val LocalTweenManager = staticCompositionLocalOf<TweenManager> { error("No tween manager in scope") }

suspend fun Tween.launch(manager: TweenManager) {
    setCallbackTriggers(TweenCallback.END)
    suspendCancellableCoroutine { continuation ->
        setCallback { type, source ->
            continuation.resume(Unit)
        }
        start(manager)
        continuation.invokeOnCancellation {
            manager.killTarget(target)
        }
    }
}
