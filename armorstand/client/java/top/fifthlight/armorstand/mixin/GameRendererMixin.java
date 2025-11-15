package top.fifthlight.armorstand.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fifthlight.armorstand.PlayerRenderer;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract float getFarPlaneDistance();

    @Inject(method = "getBasicProjectionMatrix(F)Lorg/joml/Matrix4f;", at = @At("HEAD"), cancellable = true)
    public void overrideProjectionMatrix(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
        var transform = PlayerRenderer.getCurrentCameraTransform();
        if (transform != null) {
            var aspectRadio = (float) client.getWindow().getFramebufferWidth() / (float) client.getWindow().getFramebufferHeight();
            var matrix = new Matrix4f();
            transform.getMatrix(matrix, aspectRadio, getFarPlaneDistance());
            cir.setReturnValue(matrix);
        }
    }
}
