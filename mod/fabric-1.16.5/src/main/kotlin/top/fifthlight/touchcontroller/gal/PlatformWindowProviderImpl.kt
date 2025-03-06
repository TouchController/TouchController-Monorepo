package top.fifthlight.touchcontroller.gal

import net.minecraft.client.util.Window
import org.lwjgl.glfw.GLFWNativeWayland
import org.lwjgl.glfw.GLFWNativeWin32
import org.lwjgl.glfw.GLFWNativeX11

class PlatformWindowProviderImpl(
    private val inner: Window,
) : PlatformWindowProvider {
    override val windowWidth: Int
        get() = inner.width
    override val windowHeight: Int
        get() = inner.height

    override val platform: GlfwPlatform<*> by lazy {
        val systemName = System.getProperty("os.name")
        when {
            systemName.startsWith(
                "Windows",
                ignoreCase = true
            ) -> GlfwPlatform.Win32(NativeWindow.Win32(GLFWNativeWin32.glfwGetWin32Window(inner.handle)))

            systemName.startsWith("Windows", ignoreCase = true) -> GlfwPlatform.Cocoa
            systemName.startsWith("Linux", ignoreCase = true) -> {
                val waylandDisplay = GLFWNativeWayland.glfwGetWaylandDisplay()
                if (waylandDisplay != 0L) {
                    return@lazy GlfwPlatform.Wayland(
                        NativeWindow.Wayland(
                            displayPointer = GLFWNativeWayland.glfwGetWaylandDisplay(),
                            surfacePointer = GLFWNativeWayland.glfwGetWaylandWindow(inner.handle),
                        )
                    )
                }

                val x11Display = GLFWNativeX11.glfwGetX11Display()
                if (x11Display != 0L) {
                    return@lazy GlfwPlatform.X11
                }

                GlfwPlatform.Unknown
            }

            else -> GlfwPlatform.Unknown
        }
    }
}
