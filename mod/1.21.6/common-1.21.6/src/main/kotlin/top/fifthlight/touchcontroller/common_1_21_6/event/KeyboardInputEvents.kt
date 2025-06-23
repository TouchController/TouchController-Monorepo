package top.fifthlight.touchcontroller.common_1_21_6.event

import net.minecraft.client.Minecraft
import net.minecraft.client.player.KeyboardInput
import net.minecraft.world.entity.player.Input
import net.minecraft.world.phys.Vec2
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.fifthlight.touchcontroller.common.model.ControllerHudModel

object KeyboardInputEvents : KoinComponent {
    private val controllerHudModel: ControllerHudModel by inject()

    fun onEndTick(input: KeyboardInput): Vec2 {
        val client = Minecraft.getInstance()
        if (client.screen != null) {
            return input.moveVector
        }

        val result = controllerHudModel.result

        input.keyPresses = Input(
            input.keyPresses.forward() || result.forward > 0.5f || (result.boatLeft && result.boatRight),
            input.keyPresses.backward() || result.forward < -0.5f,
            input.keyPresses.left() || result.left > 0.5f || (!result.boatLeft && result.boatRight),
            input.keyPresses.right() || result.left < -0.5f || (result.boatLeft && !result.boatRight),
            input.keyPresses.jump(),
            input.keyPresses.shift(),
            input.keyPresses.sprint(),
        )
        return Vec2(
            (input.moveVector.x + result.left).coerceIn(-1f, 1f),
            (input.moveVector.y + result.forward).coerceIn(-1f, 1f),
        )
    }
}
