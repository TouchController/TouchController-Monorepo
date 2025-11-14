package top.fifthlight.touchcontroller.common.gal

import kotlinx.collections.immutable.PersistentList
import top.fifthlight.combine.data.Text

data class ChatMessage(
    val message: Text,
)

interface ChatMessageProvider {
    fun getMessages(): PersistentList<ChatMessage>
    fun sendMessage(message: String)
}