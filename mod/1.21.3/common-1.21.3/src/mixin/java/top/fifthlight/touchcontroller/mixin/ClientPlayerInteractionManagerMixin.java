package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.BlockHitResult;
import org.koin.java.KoinJavaComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.fifthlight.combine.platform_1_21_3_1_21_4.ItemFactoryImplKt;
import top.fifthlight.touchcontroller.common.config.GlobalConfigHolder;
import top.fifthlight.touchcontroller.common.helper.CrosshairTargetHelper;
import top.fifthlight.touchcontroller.helper.GameModeWithBreakingProgress;
import top.fifthlight.touchcontroller.helper.SyncableGameMode;

@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin implements GameModeWithBreakingProgress, SyncableGameMode {
    @Shadow
    private float destroyProgress;
    @Shadow
    @Final
    private Minecraft minecraft;
    @Unique
    private boolean resetPlayerLookTarget;
    @Unique
    private float prevYaw;
    @Unique
    private float prevPitch;

    @Shadow
    protected abstract void ensureHasSentCarriedItem();

    @Override
    public float touchcontroller$getBreakingProgress() {
        return destroyProgress;
    }

    @Override
    public void touchcontroller$callSyncSelectedSlot() {
        ensureHasSentCarriedItem();
    }

    @Shadow
    protected abstract void startPrediction(ClientLevel world, PredictiveAction packetCreator);

    @Unique
    private boolean crosshairAimingContainItem(Item item) {
        GlobalConfigHolder globalConfigHolder = KoinJavaComponent.get(GlobalConfigHolder.class);
        var globalConfig = globalConfigHolder.getConfig().getValue();
        return ItemFactoryImplKt.contains(globalConfig.getItem().getCrosshairAimingItems(), item);
    }

    @Unique
    public void interactBefore(Player player, InteractionHand hand) {
        var itemStack = player.getItemInHand(hand);
        if (!crosshairAimingContainItem(itemStack.getItem())) {
            return;
        }

        prevYaw = player.getYRot();
        prevPitch = player.getXRot();

        var rotation = CrosshairTargetHelper.calculatePlayerRotation(CrosshairTargetHelper.INSTANCE.getLastCrosshairDirection());
        float yaw = rotation.getFirst();
        float pitch = rotation.getSecond();
        this.startPrediction(this.minecraft.level, sequence -> new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround(), player.horizontalCollision));
        player.setYRot(yaw);
        player.setXRot(pitch);
        resetPlayerLookTarget = true;
    }

    @Unique
    public void interactAfter(Player player) {
        if (!resetPlayerLookTarget) {
            return;
        }
        resetPlayerLookTarget = false;
        player.setYRot(prevYaw);
        player.setXRot(prevPitch);
        this.startPrediction(this.minecraft.level, sequence -> new ServerboundMovePlayerPacket.Rot(player.getYRot(), player.getXRot(), player.onGround(), player.horizontalCollision));
    }

    @Inject(
            method = "useItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startPrediction(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/multiplayer/prediction/PredictiveAction;)V",
                    ordinal = 0
            )
    )
    public void interactBlockBefore(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        interactBefore(player, hand);
    }

    @Inject(
            method = "useItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startPrediction(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/multiplayer/prediction/PredictiveAction;)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    public void interactBlockAfter(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        interactAfter(player);
    }

    @Inject(
            method = "useItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startPrediction(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/multiplayer/prediction/PredictiveAction;)V",
                    ordinal = 0
            )
    )
    public void interactItemBefore(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        interactBefore(player, hand);
    }

    @Inject(
            method = "useItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startPrediction(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/multiplayer/prediction/PredictiveAction;)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    public void interactItemAfter(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        interactAfter(player);
    }
}
