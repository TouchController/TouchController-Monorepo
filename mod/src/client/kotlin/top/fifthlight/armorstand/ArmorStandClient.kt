package top.fifthlight.armorstand

import net.minecraft.client.option.KeyBinding
import org.lwjgl.glfw.GLFW

interface ArmorStandClient : ArmorStand {
    companion object {
        lateinit var instance: ArmorStandClient

        val configKeyBinding = KeyBinding(
            "armorstand.keybinding.config",
            GLFW.GLFW_KEY_I,
            "armorstand.name"
        )
        val animationKeyBinding = KeyBinding(
            "armorstand.keybinding.animation",
            GLFW.GLFW_KEY_K,
            "armorstand.name"
        )
        val modelSwitchKeyBinding = KeyBinding(
            "armorstand.keybinding.model_switch",
            GLFW.GLFW_KEY_U,
            "armorstand.name"
        )
    }

    val debug: Boolean
    val debugBone: Boolean
}
