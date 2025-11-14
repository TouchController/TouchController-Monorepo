package top.fifthlight.touchcontroller.common.gal

import java.io.InputStream
import java.nio.file.Path

interface NativeLibraryPathGetter {
    fun getNativeLibraryPath(path: String, debugPath: Path?): InputStream?
}