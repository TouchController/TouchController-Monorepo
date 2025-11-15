package top.fifthlight.armorstand.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import top.fifthlight.armorstand.event.ScreenEvents;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Mouse;isCursorLocked()Z"))
    public boolean isCursorLocked(Mouse instance, Operation<Boolean> original) {
        var screen = client.currentScreen;
        return original.call(instance) && ScreenEvents.MOVE_VIEW.getInvoker().onViewMoved(screen);
    }
}
