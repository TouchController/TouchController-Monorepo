package top.fifthlight.touchcontroller.common.gal

sealed class NativeWindow {
    data class Win32(
        /**
         * Win32 handle
         */
        val handle: Long,
    ) : NativeWindow()

    data class Wayland(
        /**
         * Pointer to wl_display
         */
        val displayPointer: Long,
        /**
         * Pointer to wl_surface
         */
        val surfacePointer: Long,
    ) : NativeWindow()
}

sealed class GlfwPlatform<Window : NativeWindow> {
    abstract val nativeWindow: Window

    data class Win32(override val nativeWindow: NativeWindow.Win32) : GlfwPlatform<NativeWindow.Win32>()

    data class Wayland(override val nativeWindow: NativeWindow.Wayland) : GlfwPlatform<NativeWindow.Wayland>()

    data object Cocoa : GlfwPlatform<NativeWindow>() {
        override val nativeWindow: NativeWindow
            get() = error("Not yet implemented")
    }

    data object X11 : GlfwPlatform<NativeWindow>() {
        override val nativeWindow: NativeWindow
            get() = error("Not yet implemented")
    }

    data object Unknown : GlfwPlatform<NativeWindow>() {
        override val nativeWindow: NativeWindow
            get() = error("Unsupported platform!")
    }
}

interface PlatformWindowProvider {
    val platform: GlfwPlatform<*>
    val windowWidth: Int
    val windowHeight: Int
}
