package top.fifthlight.touchcontroller.common.ext

import kotlin.random.Random
import kotlin.uuid.Uuid

/**
 * Generate an uuid using default random generator.
 *
 * Uuid.random() uses secure random number generator, and can block the program.
 * Most cases in Minecraft don't need such a secure uuid generator.
 */
fun fastRandomUuid(): Uuid = Uuid.fromLongs(Random.nextLong(), Random.nextLong())
