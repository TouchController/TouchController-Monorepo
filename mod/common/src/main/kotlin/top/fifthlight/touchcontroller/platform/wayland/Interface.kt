package top.fifthlight.touchcontroller.platform.wayland

object Interface {
    @JvmStatic
    external fun init(displayHandle: Long, windowHandle: Long)

    @JvmStatic
    external fun resize(width: Int, height: Int)

    @JvmStatic
    external fun pollEvent(buffer: ByteArray): Int
}