package top.fifthlight.blazerod.util.glsl

import net.minecraft.client.gl.Defines
import kotlin.math.max

object GlslExtensionProcessor {
    private val versionPattern = """version\((?<condition><=|>=|>|<)(?<major>\d+)\.(?<minor>\d+)\)""".toRegex()
    private val definePattern = """defined\((?<define>\w+)\)""".toRegex()

    data class Context(
        val majorVersion: Int,
        val minorVersion: Int,
        val defines: Defines,
    )

    private fun Context.isMet(conditions: String): Boolean = conditions.split("&&")
        .asSequence()
        .map(String::trim)
        .all { condition ->
            versionPattern.matchEntire(condition)?.let {
                val (operator, major, minor) = it.destructured
                val majorNum = major.toIntOrNull() ?: throw IllegalArgumentException("Invalid major version $major")
                val minorNum = minor.toIntOrNull() ?: throw IllegalArgumentException("Invalid minor version $minor")
                when (operator) {
                    "<" -> majorVersion < majorNum || (majorVersion == majorNum && minorVersion < minorNum)
                    "<=" -> majorVersion < majorNum || (majorVersion == majorNum && minorVersion <= minorNum)
                    ">" -> majorVersion > majorNum || (majorVersion == majorNum && minorVersion > minorNum)
                    ">=" -> majorVersion > majorNum || (majorVersion == majorNum && minorVersion >= minorNum)
                    else -> throw IllegalArgumentException("Invalid operator $operator")
                }
            } ?: definePattern.matchEntire(condition)?.let {
                val (define) = it.destructured
                define in defines.flags || define in defines.values.keys
            } ?: throw IllegalArgumentException("Invalid condition $condition")
        }

    private val glslVersionPattern = """#version (?<version>\d+)(|(?<profile>core|compatibility))""".toRegex()
    private val blazerodVersionPattern =
        """#blazerod_version\s+(?<condition>\S+)\s*\?\s*(?<first>\d+)\s*:\s*(?<second>\d+)""".toRegex()
    private val blazerodExtensionPattern =
        """#blazerod_extension\s+(?<condition>[^;]+)\s*;\s*(?<extension>\w+)\s*:\s*(?<status>(require|enable|warn|disable))""".toRegex()

    @JvmStatic
    fun process(context: Context, source: String): String {
        var finalVersion = 0
        var needProcess = false
        val extensions = mutableMapOf<String, String>()
        val processedLines = StringBuilder()
        for (line in source.lines()) {
            val processed = run {
                when {
                    glslVersionPattern.matches(line) -> {
                        val (version) = glslVersionPattern.matchEntire(line)?.destructured ?: return@run line
                        finalVersion = max(finalVersion, version.toIntOrNull() ?: return@run line)
                        "/* $line */"
                    }

                    blazerodVersionPattern.matches(line) -> {
                        val (condition, first, second) = blazerodVersionPattern.matchEntire(line)?.destructured
                            ?: return@run line
                        finalVersion = if (context.isMet(condition)) {
                            max(
                                finalVersion,
                                first.toIntOrNull() ?: throw IllegalArgumentException("Invalid version $first")
                            )
                        } else {
                            max(
                                finalVersion,
                                second.toIntOrNull() ?: throw IllegalArgumentException("Invalid version $second")
                            )
                        }
                        needProcess = true
                        "/* $line */"
                    }

                    blazerodExtensionPattern.matches(line) -> {
                        val (condition, extension, status) = blazerodExtensionPattern.matchEntire(line)?.destructured
                            ?: return@run line
                        if (context.isMet(condition)) {
                            extensions[extension] = status
                        }
                        needProcess = true
                        "/* $line */"
                    }

                    else -> line
                }
            }
            processedLines.appendLine(processed)
        }
        if (!needProcess) {
            return source
        }
        return buildString {
            appendLine("#version $finalVersion")
            for ((extension, status) in extensions) {
                appendLine("#extension $extension : $status")
            }
            appendLine("#line 1 0")
            append(processedLines)
        }
    }
}
