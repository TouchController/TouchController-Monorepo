package top.fifthlight.armorstand.state

import com.mojang.logging.LogUtils
import kotlinx.coroutines.*
import net.minecraft.client.MinecraftClient
import top.fifthlight.armorstand.ArmorStand
import top.fifthlight.armorstand.config.ConfigHolder
import top.fifthlight.armorstand.manage.ModelManagerHolder
import top.fifthlight.armorstand.vmc.VmcMarionetteManager
import top.fifthlight.blazerod.api.animation.AnimationContextsFactory
import top.fifthlight.blazerod.api.animation.AnimationItem
import top.fifthlight.blazerod.api.animation.AnimationItemFactory
import top.fifthlight.blazerod.api.animation.AnimationItemInstanceFactory
import top.fifthlight.blazerod.api.loader.ModelLoaderFactory
import top.fifthlight.blazerod.api.refcount.RefCount
import top.fifthlight.blazerod.api.resource.ModelInstance
import top.fifthlight.blazerod.api.resource.ModelInstanceFactory
import top.fifthlight.blazerod.api.resource.RenderScene
import top.fifthlight.blazerod.model.Metadata
import top.fifthlight.blazerod.model.formats.ModelFileLoaders
import java.nio.file.Path
import java.util.*
import kotlin.io.path.nameWithoutExtension
import kotlin.time.measureTimedValue

object ModelInstanceManager {
    private val LOGGER = LogUtils.getLogger()
    const val INSTANCE_EXPIRE_NS: Long = 30L * 1000000000L
    private val client = MinecraftClient.getInstance()
    private val selfUuid: UUID?
        get() = client.player?.uuid
    val modelCaches = mutableMapOf<Path, Deferred<ModelCache>>()
    val modelInstanceItems = mutableMapOf<UUID, ModelInstanceItem>()
    private val modelDir
        get() = ModelManagerHolder.modelDir
    val defaultAnimationDir: Path = modelDir.resolve("animations")
    private val scope
        get() = ArmorStand.instance.scope

    private const val MAX_CACHE_FAVORITE_MODEL_ITEMS = 5
    val favoriteModelPaths = ArrayDeque<Path>()

    fun addFavoriteModelPath(path: Path) {
        if (path in favoriteModelPaths) {
            return
        }
        favoriteModelPaths.addFirst(path)
        while (favoriteModelPaths.size > MAX_CACHE_FAVORITE_MODEL_ITEMS) {
            favoriteModelPaths.removeLast()
        }
    }

    sealed class ModelCache {
        data object Failed : ModelCache()

        data class Loaded(
            val metadata: Metadata?,
            val scene: RenderScene,
            val animations: List<AnimationItem>,
            val animationSet: AnimationSet,
        ) : RefCount by scene, ModelCache()
    }

    sealed interface ModelInstanceItem {
        val path: Path

        data class Failed(
            override val path: Path,
        ) : ModelInstanceItem

        class Model(
            override val path: Path,
            val animations: List<AnimationItem>,
            var lastAccessTime: Long,
            val metadata: Metadata?,
            val instance: ModelInstance,
            var controller: ModelController,
        ) : RefCount by instance, ModelInstanceItem
    }

    private suspend fun loadModel(path: Path): ModelCache = withContext(Dispatchers.Default) {
        val (result, duration) = measureTimedValue {
            val modelPath = modelDir.resolve(path).toAbsolutePath()
            val result = try {
                ModelFileLoaders.probeAndLoad(modelPath)
            } catch (ex: Exception) {
                LOGGER.warn("Model load failed", ex)
                return@withContext ModelCache.Failed
            } ?: return@withContext ModelCache.Failed

            val model = result.model ?: return@withContext ModelCache.Failed
            LOGGER.info("Model metadata: ${result.metadata}")

            val scene = try {
                val loader = ModelLoaderFactory.create()
                loader.loadModel(model) ?: run {
                    LOGGER.warn("Model contains no scene")
                    return@withContext ModelCache.Failed
                }
            } catch (ex: Exception) {
                LOGGER.warn("Model scene load failed", ex)
                return@withContext ModelCache.Failed
            }
            val animations = result.animations?.map { AnimationItemFactory.load(scene, it) } ?: listOf()

            val defaultAnimationSet = AnimationSetLoader.load(scene, animations, defaultAnimationDir)
            val modelAnimation = modelPath.parent?.let { parentPath ->
                listOf(
                    modelPath.nameWithoutExtension,
                    modelPath.fileName.toString(),
                ).asSequence().map {
                    parentPath.resolve("$it.animations")
                }.fold(defaultAnimationSet) { acc, path ->
                    acc + AnimationSetLoader.load(scene, animations, path)
                }
            } ?: defaultAnimationSet

            ModelCache.Loaded(
                scene = scene,
                animations = animations,
                metadata = result.metadata,
                animationSet = modelAnimation,
            )
        }
        LOGGER.info("Model $path loaded, duration: $duration")
        result
    }

    private fun loadCache(path: Path): Deferred<ModelCache> = modelCaches.getOrPut(path) {
        scope.async {
            val item = loadModel(path)
            (item as? ModelCache.Loaded)?.increaseReferenceCount()
            item
        }
    }

    fun getSelfItem(load: Boolean) = selfUuid?.let { get(it, time = null, load = load) }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun get(uuid: UUID, time: Long?, load: Boolean = true): ModelInstanceItem? {
        val isSelf = uuid == selfUuid
        if (!isSelf && !ConfigHolder.config.value.showOtherPlayerModel) {
            return null
        }

        val path = ClientModelPathManager.getPath(uuid) ?: return null

        val lastAccessTime = if (isSelf) {
            -1
        } else {
            time
        }

        val item = modelInstanceItems[uuid]
        if (item != null) {
            if (item.path == path) {
                (item as? ModelInstanceItem.Model)?.let {
                    lastAccessTime?.let { time ->
                        it.lastAccessTime = time
                    }
                }
                return item
            } else if (lastAccessTime != null) {
                val prevItem = modelInstanceItems.remove(uuid)
                (prevItem as? ModelInstanceItem.Model)?.decreaseReferenceCount()
            }
        }
        if (lastAccessTime == null) {
            return null
        }
        if (!load) {
            return null
        }

        val cacheDeferred = loadCache(path)
        if (!cacheDeferred.isCompleted) {
            return null
        }
        val newItem = when (val cache = cacheDeferred.getCompleted()) {
            ModelCache.Failed -> ModelInstanceItem.Failed(path = path)
            is ModelCache.Loaded -> {
                val scene = cache.scene
                ModelInstanceItem.Model(
                    path = path,
                    animations = cache.animations,
                    metadata = cache.metadata,
                    lastAccessTime = lastAccessTime,
                    instance = ModelInstanceFactory.of(scene),
                    controller = run {
                        val vmcRunning = VmcMarionetteManager.state.value is VmcMarionetteManager.State.Running
                        val animationSet = FullAnimationSet.from(cache.animationSet)
                        val animation = cache.animations.firstOrNull()
                        when {
                            isSelf && vmcRunning -> ModelController.Vmc(scene)

                            animationSet != null -> ModelController.LiveSwitched(
                                AnimationContextsFactory.create().base(),
                                scene,
                                animationSet,
                            )

                            animation != null -> ModelController.Predefined(
                                AnimationContextsFactory.create().base(),
                                AnimationItemInstanceFactory.of(animation),
                            )

                            else -> ModelController.LiveUpdated(scene)
                        }
                    },
                ).also {
                    it.increaseReferenceCount()
                }
            }
        }
        val prevItem = modelInstanceItems.remove(uuid)
        (prevItem as? ModelInstanceItem.Model)?.decreaseReferenceCount()
        modelInstanceItems[uuid] = newItem
        LOGGER.info("Loaded model $path for uuid $uuid")
        return newItem
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun cleanAll() {
        modelInstanceItems.values.forEach {
            (it as? ModelInstanceItem.Model)?.decreaseReferenceCount()
        }
        modelInstanceItems.clear()
        modelCaches.values.forEach { item ->
            if (item.isCompleted) {
                val item = item.getCompleted() as? ModelCache.Loaded
                item?.decreaseReferenceCount()
            } else {
                item.cancel()
            }
        }
        modelCaches.clear()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun cleanup(time: Long) {
        val usedPaths = mutableSetOf<Path>()

        // cleaned unused model instances
        modelInstanceItems.entries.removeIf { (uuid, item) ->
            if (uuid == selfUuid) {
                if (item.path == ClientModelPathManager.selfPath) {
                    return@removeIf false
                } else {
                    (item as? ModelInstanceItem.Model)?.decreaseReferenceCount()
                    return@removeIf true
                }
            }
            val pathInvalid = item.path != ClientModelPathManager.getPath(uuid)
            if (pathInvalid) {
                (item as? ModelInstanceItem.Model)?.decreaseReferenceCount()
                return@removeIf true
            }
            when (item) {
                is ModelInstanceItem.Failed -> false
                is ModelInstanceItem.Model -> {
                    val timeSinceLastUsed = time - item.lastAccessTime
                    val expired = timeSinceLastUsed > INSTANCE_EXPIRE_NS
                    if (expired) {
                        item.decreaseReferenceCount()
                    } else {
                        usedPaths.add(item.path)
                    }
                    expired
                }
            }
        }

        // cleaned unused model caches
        modelCaches.entries.removeIf { (path, item) ->
            if (path == ClientModelPathManager.selfPath) {
                return@removeIf false
            }
            if (path in favoriteModelPaths) {
                return@removeIf false
            }
            val remove = path !in usedPaths
            if (remove && item.isCompleted) {
                val item = item.getCompleted() as? ModelCache.Loaded
                item?.decreaseReferenceCount()
            }
            remove
        }
    }
}
