package top.fifthlight.blazerod.extension

import com.mojang.blaze3d.systems.CommandEncoder
import top.fifthlight.blazerod.systems.ComputePass
import java.util.function.Supplier

fun CommandEncoder.createComputePass(label: Supplier<String>): ComputePass =
    (this as CommandEncoderExt).`blazerod$createComputePass`(label)

fun CommandEncoder.memoryBarrier(barriers: Int) =
    (this as CommandEncoderExt).`blazerod$memoryBarrier`(barriers)
