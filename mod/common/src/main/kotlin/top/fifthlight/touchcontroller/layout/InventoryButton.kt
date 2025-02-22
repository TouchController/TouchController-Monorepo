package top.fifthlight.touchcontroller.layout

import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.gal.KeyBindingType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
private val inventoryButtonId = Uuid.parse("971c03a2-790b-4f3c-83d9-685233a89345")

@OptIn(ExperimentalUuidApi::class)
fun Context.InventoryButton() {
    val (_, _, release) = Button(id = inventoryButtonId) { clicked ->
        if (clicked) {
            Texture(texture = Textures.OUTSIDE_INVENTORY_INVENTORY_ACTIVE)
        } else {
            Texture(texture = Textures.OUTSIDE_INVENTORY_INVENTORY)
        }
    }
    if (release) {
        keyBindingHandler.getState(KeyBindingType.INVENTORY).clicked = true
    }
}