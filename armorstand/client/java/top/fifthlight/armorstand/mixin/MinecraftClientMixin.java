package top.fifthlight.armorstand.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import top.fifthlight.armorstand.event.ScreenEvents;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @WrapWithCondition(
            method = "setScreen",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Mouse;unlockCursor()V")
    )
    private boolean wrapUnlockCursor(Mouse mouse, Screen screen) {
        if (screen != null) {
            return ScreenEvents.UNLOCK_CURSOR.getInvoker().onMouseUnlocked(screen);
        }
        return true;
    }
}
