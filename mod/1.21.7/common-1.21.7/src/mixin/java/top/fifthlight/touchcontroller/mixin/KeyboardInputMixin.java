package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.touchcontroller.common_1_21_6_1_21_8.event.KeyboardInputEvents;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Inject(
            at = @At("TAIL"),
            method = "tick"
    )
    private void tick(CallbackInfo info) {
        var moveVector = KeyboardInputEvents.INSTANCE.onEndTick((KeyboardInput) (Object) this);
        ((ClientInputAccessor) this).touchcontroller$setMoveVector(moveVector);
    }
}
