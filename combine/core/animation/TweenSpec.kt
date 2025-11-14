package top.fifthlight.combine.animation

import aurelienribon.tweenengine.Tween
import aurelienribon.tweenengine.TweenEquation
import aurelienribon.tweenengine.equations.Quint

interface TweenSpec {
    val tweenType: Int?
        get() = null
    val duration: Float?
        get() = null
    val equations: TweenEquation?
        get() = null

    fun apply(tween: Tween)

    class Chained(
        val first: TweenSpec,
        val second: TweenSpec,
    ) : TweenSpec {
        override val tweenType: Int?
            get() = second.tweenType ?: first.tweenType
        override val duration: Float?
            get() = second.duration ?: first.duration
        override val equations: TweenEquation?
            get() = second.equations ?: first.equations

        override fun apply(tween: Tween) {
            first.apply(tween)
            second.apply(tween)
        }
    }

    interface Full : TweenSpec {
        override val tweenType: Int
        override val duration: Float
    }

    class Default(val next: TweenSpec) : Full {
        override val tweenType: Int
            get() = next.tweenType ?: Base.tweenType
        override val duration: Float
            get() = next.duration ?: Base.duration
        override val equations: TweenEquation?
            get() = next.equations ?: Base.equations

        override fun apply(tween: Tween) = next.apply(tween)
    }

    object Base : Full {
        override val tweenType: Int = 0
        override val duration: Float = .3f
        override val equations: TweenEquation = Quint.OUT
        override fun apply(tween: Tween) = Unit
    }

    operator fun plus(other: TweenSpec): TweenSpec = Chained(
        first = this,
        second = other
    )
}

fun TweenSpec.toFull(): TweenSpec.Full = this as? TweenSpec.Full ?: TweenSpec.Default(this)

fun repeat(
    count: Int = Tween.INFINITY,
    yoyo: Boolean = false,
    delay: Float = 0f,
) = object : TweenSpec {
    override fun apply(tween: Tween) {
        if (yoyo) {
            tween.repeat(count, delay)
        } else {
            tween.repeatYoyo(count, delay)
        }
    }
}
fun repeatForever(delay: Float = 0f) = repeat(delay = delay)

fun duration(seconds: Float) = object : TweenSpec {
    override val duration: Float = seconds
    override fun apply(tween: Tween) = Unit
}

fun equation(equation: TweenEquation) = object : TweenSpec {
    override val equations: TweenEquation = equation
    override fun apply(tween: Tween) = Unit
}

fun quintIn() = equation(Quint.IN)
fun quintOut() = equation(Quint.OUT)
fun quintInOut() = equation(Quint.INOUT)
