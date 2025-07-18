package top.fifthlight.touchcontroller.gal

import net.minecraft.client.KeyMapping
import top.fifthlight.touchcontroller.common_1_21_x.gal.AbstractKeyBindingHandlerImpl
import top.fifthlight.touchcontroller.mixin.KeyMappingGetterMixin

object KeyBindingHandlerImpl : AbstractKeyBindingHandlerImpl() {
    override fun getKeyBinding(name: String): KeyMapping? = KeyMapping.get(name)

    override fun getAllKeyBinding(): Map<String, KeyMapping> =
        KeyMappingGetterMixin.`touchcontroller$getAllKeyMappings`()
}
