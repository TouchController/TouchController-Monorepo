package top.fifthlight.armorstand.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fifthlight.armorstand.PlayerRenderer;
import top.fifthlight.armorstand.config.ConfigHolder;

@Mixin(Camera.class)
public class CameraMixin {
    @Shadow
    private Entity focusedEntity;

    @Shadow
    private float lastTickProgress;

    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F"))
    public float thirdPersonDistance(float f) {
        return f * ConfigHolder.INSTANCE.getConfig().getValue().getThirdPersonDistanceScale();
    }

    @Inject(method = "getPitch", at = @At("HEAD"), cancellable = true)
    public void wrapGetPitch(CallbackInfoReturnable<Float> cir) {
        var transform = PlayerRenderer.getCurrentCameraTransform();
        if (transform != null) {
            cir.setReturnValue(transform.getRotationEulerAngles().x());
        }
    }

    @Inject(method = "getYaw", at = @At("HEAD"), cancellable = true)
    public void wrapGetYaw(CallbackInfoReturnable<Float> cir) {
        var transform = PlayerRenderer.getCurrentCameraTransform();
        if (transform != null) {
            cir.setReturnValue(transform.getRotationEulerAngles().y());
        }
    }

    @Inject(method = "getRotation", at = @At("HEAD"), cancellable = true)
    public void wrapGetRotation(CallbackInfoReturnable<Quaternionf> cir) {
        var transform = PlayerRenderer.getCurrentCameraTransform();
        if (transform != null) {
            cir.setReturnValue(transform.getRotationQuaternion());
        }
    }

    @Inject(method = "getPos", at = @At("HEAD"), cancellable = true)
    public void wrapGetPos(CallbackInfoReturnable<Vec3d> cir) {
        var transform = PlayerRenderer.getCurrentCameraTransform();
        if (transform != null) {
            var tickProgress = (double) this.lastTickProgress;
            var position = transform.getPosition();
            cir.setReturnValue(new Vec3d(
                    position.x() + MathHelper.lerp(tickProgress, focusedEntity.lastX, focusedEntity.getX()),
                    position.y() + MathHelper.lerp(tickProgress, focusedEntity.lastY, focusedEntity.getY()),
                    position.z() + MathHelper.lerp(tickProgress, focusedEntity.lastZ, focusedEntity.getZ())
            ));
        }
    }
}
