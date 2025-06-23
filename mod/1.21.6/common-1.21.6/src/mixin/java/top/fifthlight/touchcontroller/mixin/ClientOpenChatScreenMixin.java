package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import top.fifthlight.touchcontroller.helper.ChatScreenOpenable;

@Mixin(Minecraft.class)
public abstract class ClientOpenChatScreenMixin implements ChatScreenOpenable {
    @Shadow
    protected abstract void openChatScreen(String pDefaultText);

    @Override
    public void touchcontroller$openChatScreen(String text) {
        openChatScreen(text);
    }
}
