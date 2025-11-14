package top.fifthlight.combine.backend.swing

import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.paint.Canvas
import top.fifthlight.combine.paint.Color
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntRect
import top.fifthlight.data.IntSize
import top.fifthlight.data.Offset
import top.fifthlight.data.Rect
import top.fifthlight.data.Size
import java.awt.Graphics2D

class SwingCanvas(private val graphics: Graphics2D): Canvas {
    override fun pushState() {
        TODO("Not yet implemented")
    }

    override fun popState() {
        TODO("Not yet implemented")
    }

    override fun translate(x: Int, y: Int) {
        TODO("Not yet implemented")
    }

    override fun translate(x: Float, y: Float) {
        TODO("Not yet implemented")
    }

    override fun rotate(degrees: Float) {
        TODO("Not yet implemented")
    }

    override fun scale(x: Float, y: Float) {
        TODO("Not yet implemented")
    }

    override fun fillRect(
        offset: IntOffset,
        size: IntSize,
        color: Color,
    ) {
        TODO("Not yet implemented")
    }

    override fun fillGradientRect(
        offset: Offset,
        size: Size,
        leftTopColor: Color,
        leftBottomColor: Color,
        rightTopColor: Color,
        rightBottomColor: Color,
    ) {
        TODO("Not yet implemented")
    }

    override fun drawRect(
        offset: IntOffset,
        size: IntSize,
        color: Color,
    ) {
        TODO("Not yet implemented")
    }

    override fun drawText(
        offset: IntOffset,
        text: String,
        color: Color,
    ) {
        TODO("Not yet implemented")
    }

    override fun drawText(
        offset: IntOffset,
        width: Int,
        text: String,
        color: Color,
    ) {
        TODO("Not yet implemented")
    }

    override fun drawText(
        offset: IntOffset,
        text: Text,
        color: Color,
    ) {
        TODO("Not yet implemented")
    }

    override fun drawText(
        offset: IntOffset,
        width: Int,
        text: Text,
        color: Color,
    ) {
        TODO("Not yet implemented")
    }

    override fun drawTexture(
        identifier: Identifier,
        dstRect: Rect,
        srcRect: Rect,
        tint: Color,
    ) {
        TODO("Not yet implemented")
    }

    override fun drawBackgroundTexture(
        identifier: Identifier,
        scale: Float,
        dstRect: Rect,
        tint: Color,
    ) {
        TODO("Not yet implemented")
    }

    override fun pushClip(absoluteArea: IntRect, relativeArea: IntRect) {
        TODO("Not yet implemented")
    }

    override fun popClip() {
        TODO("Not yet implemented")
    }
}