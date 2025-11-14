package top.fifthlight.touchcontroller.common.config

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable
import top.fifthlight.touchcontroller.common.config.condition.LayerConditions
import top.fifthlight.touchcontroller.common.control.ControllerWidget
import top.fifthlight.touchcontroller.common.ext.ControllerLayoutSerializer
import top.fifthlight.touchcontroller.common.ext.LayoutLayerSerializer


@Serializable(with = LayoutLayerSerializer::class)
data class LayoutLayer(
    val name: String = DEFAULT_LAYER_NAME,
    val widgets: PersistentList<ControllerWidget> = persistentListOf(),
    val conditions: LayerConditions = LayerConditions(),
) {
    companion object {
        const val DEFAULT_LAYER_NAME = "Unnamed layer"
    }

    operator fun plus(widget: ControllerWidget?) = widget?.let {
        copy(widgets = widgets + widget.newId())
    } ?: this
}

@JvmInline
@Serializable(with = ControllerLayoutSerializer::class)
value class ControllerLayout(
    val layers: PersistentList<LayoutLayer> = persistentListOf(),
) : PersistentList<LayoutLayer> by layers

fun controllerLayoutOf(vararg layers: LayoutLayer?) = ControllerLayout(layers.mapNotNull { it }.toPersistentList())
