package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.koin.java.KoinJavaComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.touchcontroller.common.event.RenderEvents;
import top.fifthlight.touchcontroller.common.model.ControllerHudModel;

@Mixin(Gui.class)
public abstract class InGameHudMixin {
    @Shadow
    @Final
    private static ResourceLocation CROSSHAIR_ATTACK_INDICATOR_FULL_SPRITE;
    @Shadow
    @Final
    private static ResourceLocation CROSSHAIR_ATTACK_INDICATOR_BACKGROUND_SPRITE;
    @Shadow
    @Final
    private static ResourceLocation CROSSHAIR_ATTACK_INDICATOR_PROGRESS_SPRITE;
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
            method = "renderCrosshair",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V",
                    ordinal = 0
            ),
            cancellable = true
    )
    public void renderCrosshair(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo callbackInfo) {
        boolean shouldRender = RenderEvents.INSTANCE.shouldRenderCrosshair();
        if (!shouldRender) {
            if (this.minecraft.options.attackIndicator().get() == AttackIndicatorStatus.CROSSHAIR) {
                float attackCooldownProgress = this.minecraft.player.getAttackStrengthScale(0.0f);
                boolean renderFullTexture = false;
                if (this.minecraft.crosshairPickEntity != null && this.minecraft.crosshairPickEntity instanceof LivingEntity && attackCooldownProgress >= 1.0f) {
                    renderFullTexture = this.minecraft.player.getCurrentItemAttackStrengthDelay() > 5.0f && this.minecraft.crosshairPickEntity.isAlive();
                }
                int x = context.guiWidth() / 2;
                int y = context.guiHeight() / 2;
                if (renderFullTexture) {
                    context.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_FULL_SPRITE, x - 8, y - 8, 16, 16);
                } else if (attackCooldownProgress < 1.0f) {
                    int progress = (int) (attackCooldownProgress * 17.0f);
                    context.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_BACKGROUND_SPRITE, x - 8, y - 2, 16, 4);
                    context.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_PROGRESS_SPRITE, 16, 4, 0, 0, x - 8, y - 2, progress, 4);
                }
            }
            callbackInfo.cancel();
        }
    }

    @Inject(
            method = "renderItemHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V",
                    ordinal = 2
            )
    )
    private void renderHotbar(GuiGraphics context, DeltaTracker deltaTracker, CallbackInfo ci) {
        var player = minecraft.player;
        if (player != null) {
            var controllerHudModel = (ControllerHudModel) KoinJavaComponent.get(ControllerHudModel.class);
            var inventory = controllerHudModel.getResult().getInventory();
            var slots = inventory.getSlots();
            var x = (context.guiWidth() - 182) / 2 + 1;
            var y = context.guiHeight() - 22 + 1;
            for (int i = 0; i < 9; i++) {
                var stack = player.getInventory().getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                var slot = slots[i];
                var progress = slot.getProgress();
                var height = (int) (16 * progress);
                context.fill(x + 20 * i + 2, y + 18 - height, x + 20 * i + 18, y + 18, 0xFF00BB00);
            }
        }
    }
}