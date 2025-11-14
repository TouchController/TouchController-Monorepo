package top.fifthlight.touchcontroller.common.ext

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import kotlinx.serialization.serializer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.fifthlight.combine.data.*

class ItemSerializer : KSerializer<Item>, KoinComponent {
    private val itemFactory: ItemFactory by inject()

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("top.fifthlight.combine.data.Item") {
        element<Identifier>("id")
        element<Int?>("metadata")
    }

    override fun serialize(encoder: Encoder, value: Item) = encoder.encodeStructure(descriptor) {
        encodeSerializableElement(descriptor, 0, serializer<Identifier>(), value.id)
        @OptIn(ExperimentalSerializationApi::class)
        if (value is MetadataItem) {
            encodeNullableSerializableElement(descriptor, 1, serializer<Int?>(), value.metadata)
        } else {
            encodeNullableSerializableElement(descriptor, 1, serializer<Int?>(), null)
        }
    }

    override fun deserialize(decoder: Decoder): Item {
        val factory = itemFactory
        return if (factory is MetadataItemFactory) {
            decoder.decodeStructure(descriptor) {
                var id: Identifier? = null
                var metadata: Int? = null
                while (true) {
                    @OptIn(ExperimentalSerializationApi::class)
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> id = decodeSerializableElement(descriptor, 0, serializer<Identifier>())
                        1 -> metadata = decodeNullableSerializableElement(descriptor, 1, serializer<Int?>(), metadata)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
                require(id != null) { "No id provided" }
                factory.createItem(id, metadata) ?: error("Bad item identifier: $id")
            }
        } else {
            decoder.decodeStructure(descriptor) {
                var id: Identifier? = null
                while (true) {
                    @OptIn(ExperimentalSerializationApi::class)
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> id = decodeSerializableElement(descriptor, 0, serializer<Identifier>())
                        1 -> decodeNullableSerializableElement(descriptor, 1, serializer<Int?>(), null)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
                require(id != null) { "No id provided" }
                factory.createItem(id) ?: error("Bad item identifier: $id")
            }
        }
    }
}
