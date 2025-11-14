package top.fifthlight.touchcontroller.common.config.condition

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.Item
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.common.ext.ItemSerializer
import top.fifthlight.touchcontroller.common.layout.ContextInput
import kotlin.uuid.Uuid

@Serializable
@SerialName("custom")
data class CustomLayerConditionKey(
    val key: Uuid,
) : LayerConditions.Key {
    override fun isFulfilled(input: ContextInput) = key in input.customCondition
}

@Serializable
@SerialName("holding_item")
data class HoldingItemConditions(
    @Serializable(with = ItemSerializer::class)
    val item: Item,
) : LayerConditions.Key {
    override fun isFulfilled(input: ContextInput) = input.playerHandle?.matchesItemOnHand(item) ?: false
}

@Serializable
@SerialName("builtin")
data class BuiltinLayerConditionKey(
    val key: Key,
) : LayerConditions.Key {
    @Serializable
    enum class Key(val text: Identifier) {
        @SerialName("swimming")
        SWIMMING(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_SWIMMING),

        @SerialName("underwater")
        UNDERWATER(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_UNDERWATER),

        @SerialName("flying")
        FLYING(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_FLYING),

        @SerialName("can_fly")
        CAN_FLY(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_CAN_FLY),

        @SerialName("sneaking")
        SNEAKING(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_SNEAKING),

        @SerialName("sprinting")
        SPRINTING(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_SPRINTING),

        @SerialName("on_ground")
        ON_GROUND(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_ON_GROUND),

        @SerialName("no_on_ground")
        NOT_ON_GROUND(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_NOT_ON_GROUND),

        @SerialName("using_item")
        USING_ITEM(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_USING_ITEM),

        @SerialName("on_minecart")
        ON_MINECART(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_ON_MINECART),

        @SerialName("on_boat")
        ON_BOAT(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_ON_BOAT),

        @SerialName("on_pig")
        ON_PIG(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_ON_PIG),

        @SerialName("on_horse")
        ON_HORSE(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_ON_HORSE),

        @SerialName("on_camel")
        ON_CAMEL(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_ON_CAMEL),

        @SerialName("on_llama")
        ON_LLAMA(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_ON_LLAMA),

        @SerialName("on_strider")
        ON_STRIDER(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_ON_STRIDER),

        @SerialName("riding")
        RIDING(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_RIDING),

        @SerialName("block_selected")
        BLOCK_SELECTED(Texts.SCREEN_CUSTOM_CONTROL_LAYOUT_LAYERS_CONDITIONS_BLOCK_SELECTED),
    }

    override fun isFulfilled(input: ContextInput) = key in input.builtInCondition
}
