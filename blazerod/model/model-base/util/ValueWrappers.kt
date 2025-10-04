package top.fifthlight.blazerod.model.util

interface FloatWrapper {
    val value: Float
}

data class MutableFloat(
    override var value: Float = 0f,
) : FloatWrapper

interface DoubleWrapper {
    val value: Double
}

data class MutableDouble(
    override var value: Double = 0.0,
) : DoubleWrapper

interface IntWrapper {
    val value: Int
}

data class MutableInt(
    override var value: Int = 0,
) : IntWrapper

interface LongWrapper {
    val value: Long
}

data class MutableLong(
    override var value: Long = 0L,
) : LongWrapper

interface BooleanWrapper {
    val value: Boolean
}

data class MutableBoolean(
    override var value: Boolean = false,
) : BooleanWrapper
