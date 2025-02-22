package top.fifthlight.touchcontroller.control

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.fifthlight.touchcontroller.gal.GameAction
import top.fifthlight.touchcontroller.gal.KeyBindingHandler
import top.fifthlight.touchcontroller.gal.KeyBindingType
import top.fifthlight.touchcontroller.gal.PlayerHandle

@Serializable
sealed class WidgetTriggerAction {
    abstract fun trigger(player: PlayerHandle)

    @SerialName("empty")
    data object Empty : WidgetTriggerAction() {
        override fun trigger(player: PlayerHandle) = Unit
    }

    @Serializable
    @SerialName("key")
    sealed class Key : WidgetTriggerAction() {
        private companion object : KoinComponent {
            val keyBindingHandler: KeyBindingHandler by inject()
        }

        abstract val type: KeyBindingType
        protected val keyBindingState by lazy {
            keyBindingHandler.getState(type)
        }

        @Serializable
        @SerialName("click")
        data class Click(
            override val type: KeyBindingType,
            val keepInClientTick: Boolean = true,
        ) : Key() {
            override fun trigger(player: PlayerHandle) {
                if (keepInClientTick) {
                    keyBindingState.clicked = true
                } else {
                    keyBindingState.click()
                }
            }
        }

        @Serializable
        @SerialName("click")
        data class Lock(
            override val type: KeyBindingType,
            val actionType: LockActionType = LockActionType.INVERT,
        ) : Key() {
            enum class LockActionType {
                START,
                END,
                INVERT,
            }

            override fun trigger(player: PlayerHandle) {
                when (actionType) {
                    LockActionType.START -> keyBindingState.locked = true
                    LockActionType.END -> keyBindingState.locked = false
                    LockActionType.INVERT -> keyBindingState.locked = !keyBindingState.locked
                }
            }
        }
    }

    @Serializable
    @SerialName("game")
    sealed class Game : WidgetTriggerAction(), KoinComponent {
        private val gameAction: GameAction by inject()
        final override fun trigger(player: PlayerHandle) = trigger(gameAction)
        abstract fun trigger(gameAction: GameAction)

        @Serializable
        @SerialName("chat_screen")
        data object ChatMenu : Game() {
            override fun trigger(gameAction: GameAction) {
                gameAction.openGameMenu()
            }
        }

        @Serializable
        @SerialName("game_menu")
        data object GameMenu : Game() {
            override fun trigger(gameAction: GameAction) {
                gameAction.openGameMenu()
            }
        }

        @Serializable
        @SerialName("next_perspective")
        data object NextPerspective : Game() {
            override fun trigger(gameAction: GameAction) {
                gameAction.nextPerspective()
            }
        }

        @Serializable
        @SerialName("take_screenshot")
        data object TakeScreenshot : Game() {
            override fun trigger(gameAction: GameAction) {
                gameAction.takeScreenshot()
            }
        }

        @Serializable
        @SerialName("take_panorama")
        data object TakePanorama : Game() {
            override fun trigger(gameAction: GameAction) {
                gameAction.takePanorama()
            }
        }
    }

    @Serializable
    @SerialName("player")
    sealed class Player : WidgetTriggerAction() {
        @Serializable
        @SerialName("cancel_flying")
        data object CancelFlying : Player() {
            override fun trigger(player: PlayerHandle) {
                player.isFlying = false
            }
        }

        @Serializable
        @SerialName("cancel_flying")
        data object StartSprint : Player() {
            override fun trigger(player: PlayerHandle) {
                player.isSprinting = true
            }
        }

        @Serializable
        @SerialName("stop_sprint")
        data object StopSprint : Player() {
            override fun trigger(player: PlayerHandle) {
                player.isSprinting = false
            }
        }
    }
}