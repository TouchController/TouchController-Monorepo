package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.combine.platform_1_21_6_1_21_7.CanvasImpl;
import top.fifthlight.touchcontroller.common.event.RenderEvents;

@Mixin(Gui.class)
public abstract class HudRenderMixin {

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderBossOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    public void onHudRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        var canvas = new CanvasImpl(guiGraphics);
        RenderEvents.INSTANCE.onHudRender(canvas);
    }
}