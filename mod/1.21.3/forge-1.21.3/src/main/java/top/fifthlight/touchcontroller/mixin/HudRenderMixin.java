package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.combine.platform_1_21_3_1_21_4.CanvasImpl;
import top.fifthlight.touchcontroller.common.event.RenderEvents;

@Mixin(Gui.class)
public abstract class HudRenderMixin {
    @Shadow
    @Final
    private LayeredDraw layers;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initLayers(Minecraft minecraft, CallbackInfo ci) {
        layers.add(this::touchController$layer);
    }

    @Unique
    private void touchController$layer(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) {
            return;
        }
        var canvas = new CanvasImpl(guiGraphics);
        RenderEvents.INSTANCE.onHudRender(canvas);
    }
}