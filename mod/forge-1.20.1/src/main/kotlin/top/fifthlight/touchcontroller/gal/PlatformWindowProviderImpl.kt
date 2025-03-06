package top.fifthlight.touchcontroller.gal

import com.mojang.blaze3d.platform.Window
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWNativeWayland
import org.lwjgl.glfw.GLFWNativeWin32

class PlatformWindowProviderImpl(
    private val inner: Window,
) : PlatformWindowProvider {
    override val windowWidth: Int
        get() = inner.width
    override val windowHeight: Int
        get() = inner.height

    override val platform: GlfwPlatform<*> by lazy {
        when (GLFW.glfwGetPlatform()) {
            GLFW.GLFW_PLATFORM_WIN32 -> GlfwPlatform.Win32(NativeWindow.Win32(GLFWNativeWin32.glfwGetWin32Window(inner.window)))
            GLFW.GLFW_PLATFORM_COCOA -> GlfwPlatform.Cocoa
            GLFW.GLFW_PLATFORM_WAYLAND -> GlfwPlatform.Wayland(
                NativeWindow.Wayland(
                    displayPointer = GLFWNativeWayland.glfwGetWaylandDisplay(),
                    surfacePointer = GLFWNativeWayland.glfwGetWaylandWindow(inner.window),
                )
            )

            GLFW.GLFW_PLATFORM_X11 -> GlfwPlatform.X11
            else -> GlfwPlatform.Unknown
        }
    }
}
