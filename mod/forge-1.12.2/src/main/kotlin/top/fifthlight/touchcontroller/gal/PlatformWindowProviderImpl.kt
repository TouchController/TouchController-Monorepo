package top.fifthlight.touchcontroller.gal

import org.lwjgl.opengl.Display
import org.slf4j.LoggerFactory

object PlatformWindowProviderImpl : PlatformWindowProvider {
    private val logger = LoggerFactory.getLogger(PlatformWindowProviderImpl::class.java)

    override val windowWidth: Int
        get() = Display.getWidth()
    override val windowHeight: Int
        get() = Display.getHeight()

    private fun getCleanroomWindow(): Long? {
        val displayClass = runCatching {
            Class.forName("org.lwjgl.opengl.Display")
        }.getOrNull() ?: return null

        return displayClass.methods?.firstOrNull {
            it.name == "getWindow"
        }?.invoke(null) as? Long
    }

    private fun getPlainForgeDisplayImpl(): Any? {
        val displayClass = runCatching {
            Class.forName("org.lwjgl.opengl.Display")
        }.getOrNull() ?: return null

        return displayClass.declaredMethods?.firstOrNull {
            it.name == "getImplementation"
        }?.run {
            isAccessible = true
            invoke(null)
        }
    }

    private fun getWin32Handle(): Long {
        fun cleanroom(): Long? {
            val windowHandle = getCleanroomWindow()

            val glfwNativeClass = runCatching {
                Class.forName("org.lwjgl3.glfw.GLFWNativeWin32")
            }.getOrNull() ?: return null
            return glfwNativeClass.methods?.firstOrNull {
                it.name == "glfwGetWin32Window"
            }?.invoke(null, windowHandle) as Long
        }

        fun plainForge(): Long? {
            val displayImpl = getPlainForgeDisplayImpl() ?: return null
            return displayImpl.javaClass.declaredMethods.firstOrNull {
                it.name == "getHwnd"
            }?.run {
                isAccessible = true
                invoke(displayImpl) as? Long
            }
        }

        try {
            val handle = cleanroom() ?: plainForge() ?: error("Cleanroom and plain Forge failed")
            return handle
        } catch (ex: Exception) {
            logger.error("Failed to get window handle", ex)
            throw ex
        }
    }

    private fun getWaylandHandle(): Pair<Long, Long>? = runCatching {
        val windowHandle = getCleanroomWindow()

        val glfwNativeClass = runCatching {
            Class.forName("org.lwjgl3.glfw.GLFWNativeWayland")
        }.getOrNull() ?: return null
        val displayPointer = glfwNativeClass.methods?.firstOrNull {
            it.name == "glfwGetWaylandDisplay"
        }?.invoke(null) as? Long ?: return null
        val surfacePointer = glfwNativeClass.methods?.firstOrNull {
            it.name == "glfwGetWaylandWindow"
        }?.invoke(null, windowHandle) as? Long ?: return null
        Pair(displayPointer, surfacePointer)
    }.getOrNull()

    override val platform: GlfwPlatform<*> by lazy {
        val systemName = System.getProperty("os.name")
        when {
            systemName.startsWith(
                "Windows",
                ignoreCase = true
            ) -> GlfwPlatform.Win32(NativeWindow.Win32(getWin32Handle()))

            systemName.startsWith("Windows", ignoreCase = true) -> GlfwPlatform.Cocoa
            systemName.startsWith("Linux", ignoreCase = true) -> {
                val waylandHandle = getWaylandHandle()
                if (waylandHandle != null) {
                    val (displayPointer, surfacePointer) = waylandHandle
                    return@lazy GlfwPlatform.Wayland(
                        NativeWindow.Wayland(
                            displayPointer = displayPointer,
                            surfacePointer = surfacePointer,
                        )
                    )
                }

                // X11 unsupported

                GlfwPlatform.Unknown
            }

            else -> GlfwPlatform.Unknown
        }
    }
}