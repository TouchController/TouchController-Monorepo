package top.fifthlight.armorstand.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import top.fifthlight.armorstand.config.ConfigHolder;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @WrapOperation(
            method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;DDDLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;renderShadows:Z")
    )
    public boolean renderShadows(EntityRenderDispatcher instance, Operation<Boolean> original, EntityRenderState state) {
        if (!(state instanceof PlayerEntityRenderState)) {
            return original.call(instance);
        }
        if (ConfigHolder.INSTANCE.getConfig().getValue().getHidePlayerShadow()) {
            return false;
        } else {
            return original.call(instance);
        }
    }
}
