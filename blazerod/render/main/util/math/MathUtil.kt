package top.fifthlight.blazerod.util.math

import net.minecraft.util.math.Vec3d
import org.joml.Vector3d

infix fun Int.ceilDiv(other: Int) = if (this % other == 0) {
    this / other
} else {
    this / other + 1
}

infix fun Int.roundUpToMultiple(divisor: Int) = (this ceilDiv divisor) * divisor

tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) {
    a
} else {
    gcd(b, a % b)
}

fun lcm(a: Int, b: Int): Int = a * (b / gcd(a, b))

fun Vector3d.set(vec3d: Vec3d) = apply {
    x = vec3d.x
    y = vec3d.y
    z = vec3d.z
}

fun Vec3d.sub(v: Vec3d, dst: Vector3d) = dst.apply {
    x = this@sub.x - v.x
    y = this@sub.y - v.y
    z = this@sub.z - v.z
}
