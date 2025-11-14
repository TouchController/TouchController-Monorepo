package top.fifthlight.combine.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalTextFactory = staticCompositionLocalOf<TextFactory> { error("No TextFactory in context") }

interface TextBuilder {
    fun bold(bold: Boolean = true, block: TextBuilder.() -> Unit)
    fun underline(underline: Boolean = true, block: TextBuilder.() -> Unit)
    fun italic(italic: Boolean = true, block: TextBuilder.() -> Unit)
    fun append(string: String)
    fun appendWithoutStyle(text: Text)
}

interface TextFactory {
    fun build(block: TextBuilder.() -> Unit): Text
    fun literal(string: String): Text
    fun of(identifier: Identifier): Text
    fun empty(): Text
    fun format(identifier: Identifier, vararg arguments: Any?): Text
    fun toNative(text: Text): Any
}

interface Text {
    val string: String

    fun bold(): Text
    fun underline(): Text
    fun italic(): Text
    fun copy(): Text
    operator fun plus(other: Text): Text

    @Composable
    fun native(): Any = LocalTextFactory.current.toNative(this)

    companion object {
        @Composable
        fun translatable(identifier: Identifier) = LocalTextFactory.current.of(identifier)

        @Composable
        fun format(identifier: Identifier, vararg arguments: Any?): Text {
            val factory = LocalTextFactory.current
            val outArguments = Array<Any?>(arguments.size) { index ->
                val item = arguments[index]
                if (item is Text) {
                    factory.toNative(item)
                } else {
                    item
                }
            }
            return factory.format(identifier, *outArguments)
        }

        @Composable
        fun empty() = LocalTextFactory.current.empty()

        @Composable
        fun literal(string: String) = LocalTextFactory.current.literal(string)

        @Composable
        fun build(block: TextBuilder.() -> Unit) = LocalTextFactory.current.build(block)
    }
}
