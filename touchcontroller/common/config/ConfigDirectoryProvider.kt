package top.fifthlight.touchcontroller.common.config

import java.nio.file.Path

interface ConfigDirectoryProvider {
    fun getConfigDirectory(): Path
}