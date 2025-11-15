package top.fifthlight.blazerod.model

import top.fifthlight.blazerod.model.Accessor.ComponentTypeItem
import top.fifthlight.blazerod.model.Primitive.Attributes.Key

data class Primitive(
    val mode: Mode,
    val material: Material?,
    val attributes: Attributes.Primitive,
    val indices: Accessor?,
    val targets: List<Attributes.MorphTarget>,
) {
    init {
        fun Accessor.check(key: Key, count: Int? = null) {
            require(key.allowedComponentTypes.any { it.type == componentType && it.normalized == normalized }) {
                "Bad component type for usage $key: $componentType (normalized: $normalized), " +
                        "allowed are ${key.allowedComponentTypes.joinToString(", ")}"
            }
            require(type in key.allowedAccessorTypes) {
                "Bad component type for usage $key: ${type}, " +
                        "allowed are ${key.allowedAccessorTypes.joinToString(", ")}"
            }
            count?.let {
                require(this.count == it) {
                    "Bad vertex attribute count: ${this.count}, should be $count"
                }
            }
        }
        with(attributes) {
            position.check(Key.POSITION)
            normal?.check(Key.NORMAL, position.count)
            tangent?.check(Key.TANGENT, position.count)
            texcoords.forEach { it.check(Key.TEXCOORD, position.count) }
            colors.forEach { it.check(Key.COLORS, position.count) }
        }
        targets.forEach { target ->
            with(target) {
                position?.check(Key.NORMAL, attributes.position.count)
                normal?.check(Key.NORMAL, attributes.position.count)
                tangent?.check(Key.TANGENT, attributes.position.count)
                texcoords.forEach { it.check(Key.TEXCOORD, attributes.position.count) }
                colors.forEach { it.check(Key.COLORS, attributes.position.count) }
            }
        }
    }

    sealed class Attributes(
        open val position: Accessor? = null,
        val normal: Accessor? = null,
        val tangent: Accessor? = null,
        val texcoords: List<Accessor> = listOf(),
        val colors: List<Accessor> = listOf(),
        val joints: List<Accessor> = listOf(),
        val weights: List<Accessor> = listOf(),
    ) {
        class MorphTarget(
            position: Accessor? = null,
            normal: Accessor? = null,
            tangent: Accessor? = null,
            texcoords: List<Accessor> = listOf(),
            colors: List<Accessor> = listOf(),
            joints: List<Accessor> = listOf(),
            weights: List<Accessor> = listOf()
        ) : Attributes(position, normal, tangent, texcoords, colors, joints, weights)

        class Primitive(
            override val position: Accessor,
            normal: Accessor? = null,
            tangent: Accessor? = null,
            texcoords: List<Accessor> = listOf(),
            colors: List<Accessor> = listOf(),
            joints: List<Accessor> = listOf(),
            weights: List<Accessor> = listOf()
        ) : Attributes(position, normal, tangent, texcoords, colors, joints, weights)

        enum class Key(
            val allowedComponentTypes: List<ComponentTypeItem>,
            val allowedAccessorTypes: List<Accessor.AccessorType>,
            val useAsInteger: Boolean = false,
            // FIXME: tangent should be calculated from position.
            val defaultOne: Boolean = false,
        ) {
            POSITION(
                allowedComponentTypes = listOf(ComponentTypeItem(Accessor.ComponentType.FLOAT)),
                allowedAccessorTypes = listOf(Accessor.AccessorType.VEC3),
            ),
            NORMAL(
                allowedComponentTypes = listOf(ComponentTypeItem(Accessor.ComponentType.FLOAT)),
                allowedAccessorTypes = listOf(Accessor.AccessorType.VEC3),
            ),
            TANGENT(
                allowedComponentTypes = listOf(ComponentTypeItem(Accessor.ComponentType.FLOAT)),
                allowedAccessorTypes = listOf(Accessor.AccessorType.VEC4),
            ),
            TEXCOORD(
                allowedComponentTypes = listOf(
                    ComponentTypeItem(Accessor.ComponentType.FLOAT),
                    ComponentTypeItem(Accessor.ComponentType.UNSIGNED_BYTE, normalized = true),
                    ComponentTypeItem(Accessor.ComponentType.UNSIGNED_SHORT, normalized = true),
                ),
                allowedAccessorTypes = listOf(Accessor.AccessorType.VEC2),
            ),
            COLORS(
                allowedComponentTypes = listOf(
                    ComponentTypeItem(Accessor.ComponentType.FLOAT),
                    ComponentTypeItem(Accessor.ComponentType.UNSIGNED_BYTE, normalized = true),
                    ComponentTypeItem(Accessor.ComponentType.UNSIGNED_SHORT, normalized = true),
                ),
                allowedAccessorTypes = listOf(Accessor.AccessorType.VEC4, Accessor.AccessorType.VEC3),
                defaultOne = true,
            ),
            JOINTS(
                allowedComponentTypes = listOf(
                    ComponentTypeItem(Accessor.ComponentType.BYTE),
                    ComponentTypeItem(Accessor.ComponentType.SHORT),
                ),
                allowedAccessorTypes = listOf(Accessor.AccessorType.VEC4),
                useAsInteger = true,
            ),
            WEIGHTS(
                allowedComponentTypes = listOf(
                    ComponentTypeItem(Accessor.ComponentType.FLOAT),
                    ComponentTypeItem(Accessor.ComponentType.UNSIGNED_BYTE, normalized = true),
                    ComponentTypeItem(Accessor.ComponentType.UNSIGNED_SHORT, normalized = true),
                ),
                allowedAccessorTypes = listOf(Accessor.AccessorType.VEC4),
            );
        }

        operator fun contains(key: Key): Boolean = when (key) {
            Key.POSITION -> true
            Key.NORMAL -> normal != null
            Key.TANGENT -> tangent != null
            Key.TEXCOORD -> texcoords.isNotEmpty()
            Key.COLORS -> colors.isNotEmpty()
            Key.JOINTS -> joints.isNotEmpty()
            Key.WEIGHTS -> weights.isNotEmpty()
        }
    }

    enum class Mode {
        POINTS,
        LINE_STRIP,
        LINE_LOOP,
        LINES,
        TRIANGLES,
        TRIANGLE_STRIP,
        TRIANGLE_FAN,
    }
}
