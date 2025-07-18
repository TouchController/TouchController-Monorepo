package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import top.fifthlight.combine.platform_1_21_6_1_21_8.SubmittableDrawContext;

@Mixin(GuiGraphics.class)
public abstract class DrawContextMixin implements SubmittableDrawContext {
    @Shadow
    @Final
    private GuiRenderState guiRenderState;

    @Shadow
    @Final
    private GuiGraphics.ScissorStack scissorStack;

    @ModifyArg(
            method = "<init>(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/render/state/GuiRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/joml/Matrix3x2fStack;<init>(I)V",
                    ordinal = 0
            )
    )
    private static int modifyStackLimit(int stackSize) {
        return Math.max(stackSize, 64);
    }

    @Override
    public void touchcontroller$submitElement(GuiElementRenderState guiElementRenderState) {
        guiRenderState.submitGuiElement(guiElementRenderState);
    }

    @Override
    public ScreenRectangle touchcontroller$peekScissorStack() {
        return scissorStack.peek();
    }
}
