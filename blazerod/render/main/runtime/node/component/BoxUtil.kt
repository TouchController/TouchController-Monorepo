package top.fifthlight.blazerod.runtime.node.component

import net.minecraft.client.render.VertexConsumer
import org.joml.Matrix4f

fun VertexConsumer.drawBox(matrix: Matrix4f, halfSize: Float, color: Int) {
    vertex(matrix, -halfSize, -halfSize, halfSize).color(color)
    vertex(matrix, halfSize, -halfSize, halfSize).color(color)
    vertex(matrix, halfSize, halfSize, halfSize).color(color)
    vertex(matrix, -halfSize, halfSize, halfSize).color(color)

    vertex(matrix, -halfSize, -halfSize, -halfSize).color(color)
    vertex(matrix, -halfSize, halfSize, -halfSize).color(color)
    vertex(matrix, halfSize, halfSize, -halfSize).color(color)
    vertex(matrix, halfSize, -halfSize, -halfSize).color(color)

    vertex(matrix, -halfSize, -halfSize, -halfSize).color(color)
    vertex(matrix, -halfSize, -halfSize, halfSize).color(color)
    vertex(matrix, -halfSize, halfSize, halfSize).color(color)
    vertex(matrix, -halfSize, halfSize, -halfSize).color(color)

    vertex(matrix, halfSize, -halfSize, -halfSize).color(color)
    vertex(matrix, halfSize, halfSize, -halfSize).color(color)
    vertex(matrix, halfSize, halfSize, halfSize).color(color)
    vertex(matrix, halfSize, -halfSize, halfSize).color(color)

    vertex(matrix, -halfSize, halfSize, -halfSize).color(color)
    vertex(matrix, -halfSize, halfSize, halfSize).color(color)
    vertex(matrix, halfSize, halfSize, halfSize).color(color)
    vertex(matrix, halfSize, halfSize, -halfSize).color(color)

    vertex(matrix, -halfSize, -halfSize, -halfSize).color(color)
    vertex(matrix, halfSize, -halfSize, -halfSize).color(color)
    vertex(matrix, halfSize, -halfSize, halfSize).color(color)
    vertex(matrix, -halfSize, -halfSize, halfSize).color(color)
}