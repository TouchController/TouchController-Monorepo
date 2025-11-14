package top.fifthlight.touchcontroller.common.config.widget

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import top.fifthlight.touchcontroller.common.config.ConfigDirectoryProvider
import top.fifthlight.touchcontroller.common.control.ControllerWidget
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

class WidgetPresetManager : KoinComponent {
    private val logger = LoggerFactory.getLogger(WidgetPresetManager::class.java)
    private val configDirectoryProvider: ConfigDirectoryProvider = get()
    private val presetFile = configDirectoryProvider.getConfigDirectory().resolve("widget.json")
    private val json: Json by inject()

    private val _presets = MutableStateFlow(persistentListOf<ControllerWidget>())
    val presets = _presets.asStateFlow()

    @OptIn(ExperimentalSerializationApi::class)
    fun load() {
        try {
            logger.info("Reading TouchController widgets file")
            _presets.value = runCatching {
                presetFile.inputStream().use { json.decodeFromStream<List<ControllerWidget>>(it) }
            }.getOrNull()?.toPersistentList() ?: persistentListOf()
        } catch (ex: Exception) {
            logger.warn("Failed to read presets", ex)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun save(presets: PersistentList<ControllerWidget>) {
        logger.info("Saving TouchController widgets")
        presetFile.parent.createDirectories()
        presetFile.outputStream().use { json.encodeToStream<List<ControllerWidget>>(presets, it) }
        _presets.value = presets
    }
}