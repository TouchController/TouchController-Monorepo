package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.combine.platform_1_21_x.TextImpl;
import top.fifthlight.touchcontroller.common.ui.screen.ConfigScreenKt;

@Mixin(ControlsScreen.class)
public abstract class ControlsOptionsScreenMixin {
    @Inject(at = @At("TAIL"), method = "addOptions")
    protected void addOptions(CallbackInfo ci) {
        var client = Minecraft.getInstance();
        var screen = (ControlsScreen) (Object) this;
        var body = ((GameOptionsScreenMixin) this).body();
        var textObj = ConfigScreenKt.getConfigScreenButtonText();
        if (textObj instanceof TextImpl) {
            textObj = ((TextImpl) textObj).getInner();
        }
        Component text = (Component) textObj;
        body.addSmall(
                Button.builder(
                        text,
                        btn -> client.setScreen((Screen) ConfigScreenKt.getConfigScreen(screen))
                ).build(), null
        );
    }
}