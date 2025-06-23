package top.fifthlight.combine.platform_1_21_6;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiElementRenderState;

public interface SubmittableDrawContext {
    void touchcontroller$submitElement(GuiElementRenderState guiElementRenderState);
    ScreenRectangle touchcontroller$peekScissorStack();
}
