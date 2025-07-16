package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.touchcontroller.TouchController;
import top.fifthlight.touchcontroller.common.event.RenderEvents;

@Mixin(Minecraft.class)
public abstract class ClientRenderMixin {
    @Inject(method = "runTick", at = @At("HEAD"))
    public void onRenderStart(boolean tick, CallbackInfo ci) {
        if (TouchController.Companion.getLoaded()) {
            RenderEvents.onRenderStart();
        }
    }
}
