package top.fifthlight.armorstand.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.armorstand.config.ConfigHolder;

@Mixin(ArmorFeatureRenderer.class)
public class ArmorFeatureRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    public void disableRendering(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        if (ConfigHolder.INSTANCE.getConfig().getValue().getHidePlayerArmor()) {
            ci.cancel();
        }
    }
}
