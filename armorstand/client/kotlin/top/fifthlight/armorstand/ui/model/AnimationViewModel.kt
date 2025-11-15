package top.fifthlight.armorstand.ui.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import net.minecraft.client.MinecraftClient
import org.slf4j.LoggerFactory
import top.fifthlight.armorstand.PlayerRenderer
import top.fifthlight.armorstand.manage.ModelManagerHolder
import top.fifthlight.armorstand.state.ModelController
import top.fifthlight.armorstand.state.ModelInstanceManager
import top.fifthlight.armorstand.ui.state.AnimationScreenState
import top.fifthlight.blazerod.api.animation.AnimationContextsFactory
import top.fifthlight.blazerod.api.animation.AnimationItem
import top.fifthlight.blazerod.api.animation.AnimationItemFactory
import top.fifthlight.blazerod.api.animation.AnimationItemInstanceFactory
import top.fifthlight.blazerod.api.resource.ModelInstance
import top.fifthlight.blazerod.model.animation.SimpleAnimationState
import top.fifthlight.blazerod.model.formats.ModelFileLoaders
import java.lang.ref.WeakReference

class AnimationViewModel(scope: CoroutineScope) : ViewModel(scope) {
    private val _uiState = MutableStateFlow(AnimationScreenState())
    val uiState = _uiState.asStateFlow()

    companion object {
        private val logger = LoggerFactory.getLogger(AnimationViewModel::class.java)
    }

    init {
        scope.launch {
            ModelManagerHolder.instance.lastUpdateTime.collectLatest {
                _uiState.getAndUpdate {
                    it.copy(
                        externalAnimations = ModelManagerHolder.instance.getAnimations().map { item ->
                            AnimationScreenState.AnimationItem(
                                name = item.name,
                                source = AnimationScreenState.AnimationItem.Source.External(item.path),
                            )
                        },
                    )
                }
            }
        }
    }

    private fun getInstanceItem(): ModelInstanceManager.ModelInstanceItem.Model? {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return null
        val item = ModelInstanceManager.get(player.uuid, null)
        return item as? ModelInstanceManager.ModelInstanceItem.Model
    }

    fun togglePlay() {
        val instanceItem = getInstanceItem() ?: return
        val controller = (instanceItem.controller as? ModelController.AnimatedModelController) ?: return
        val animationState = (controller.animationState as? SimpleAnimationState) ?: return
        animationState.paused = !animationState.paused
    }

    fun updatePlaySpeed(speed: Float) {
        val instanceItem = getInstanceItem() ?: return
        val controller = (instanceItem.controller as? ModelController.AnimatedModelController) ?: return
        val animationState = (controller.animationState as? SimpleAnimationState) ?: return
        animationState.speed = speed
    }

    fun updateProgress(progress: Float) {
        val instanceItem = getInstanceItem() ?: return
        val controller = (instanceItem.controller as? ModelController.AnimatedModelController) ?: return
        val animationState = (controller.animationState as? SimpleAnimationState) ?: return
        animationState.seek(progress)
    }

    private var prevAnimations = WeakReference<List<AnimationItem>>(null)
    private var prevInstance = WeakReference<ModelInstance>(null)
    private var ikUpdated = false

    fun tick() {
        val instanceItem = getInstanceItem() ?: run {
            _uiState.getAndUpdate {
                it.copy(playState = AnimationScreenState.PlayState.None)
            }
            return
        }
        val animations = instanceItem.animations
        if (animations != prevAnimations.get()) {
            prevAnimations = WeakReference(animations)
            _uiState.getAndUpdate { state ->
                state.copy(embedAnimations = animations.mapIndexed { index, animation ->
                    AnimationScreenState.AnimationItem(
                        name = animation.name,
                        duration = animation.duration,
                        source = AnimationScreenState.AnimationItem.Source.Embed(index),
                    )
                })
            }
        }
        val instance = instanceItem.instance
        if (instance != prevInstance.get() || ikUpdated) {
            ikUpdated = false
            prevInstance = WeakReference(instance)
            _uiState.getAndUpdate {
                it.copy(ikList = instance.scene.ikTargetData.mapIndexed { index, component ->
                    Pair(
                        component.effectorNode.nodeName,
                        instance.getIkEnabled(index),
                    )
                })
            }
        }
        when (val controller = instanceItem.controller) {
            is ModelController.AnimatedModelController -> {
                val playState = when (val animationState = controller.animationState) {
                    is SimpleAnimationState -> {
                        val duration = animationState.duration
                        val progress = animationState.getTime()
                        if (animationState.playing) {
                            AnimationScreenState.PlayState.Playing(
                                progress = progress,
                                length = duration,
                                speed = animationState.speed,
                                readonly = false,
                            )
                        } else {
                            AnimationScreenState.PlayState.Paused(
                                progress = progress,
                                length = duration,
                                speed = animationState.speed,
                                readonly = false,
                            )
                        }
                    }

                    else -> {
                        val duration = animationState.duration
                        val progress = animationState.getTime()
                        if (animationState.playing) {
                            AnimationScreenState.PlayState.Playing(
                                progress = progress,
                                length = duration ?: 1f,
                                speed = 1f,
                                readonly = true,
                            )
                        } else {
                            AnimationScreenState.PlayState.Paused(
                                progress = progress,
                                length = duration ?: 1f,
                                speed = 1f,
                                readonly = true,
                            )
                        }
                    }
                }
                _uiState.getAndUpdate {
                    it.copy(playState = playState)
                }
            }

            else -> {
                _uiState.getAndUpdate {
                    it.copy(playState = AnimationScreenState.PlayState.None)
                }
            }
        }
    }

    fun switchAnimation(item: AnimationScreenState.AnimationItem) {
        val instanceItem = getInstanceItem() ?: return
        when (val source = item.source) {
            is AnimationScreenState.AnimationItem.Source.Embed -> {
                val index = source.index
                val animation = instanceItem.animations[index]
                instanceItem.instance.clearTransform()
                instanceItem.controller = ModelController.Predefined(
                    AnimationContextsFactory.create().base(),
                    AnimationItemInstanceFactory.of(animation),
                )
            }

            is AnimationScreenState.AnimationItem.Source.External -> {
                instanceItem.controller = ModelController.LiveUpdated(instanceItem.instance.scene)
                scope.launch {
                    try {
                        val path = ModelManagerHolder.modelDir.resolve(source.path)
                        val result = ModelFileLoaders.probeAndLoad(path)
                        val animation = result?.animations?.firstOrNull() ?: error("No animation in file")
                        val animationItem = AnimationItemFactory.load(instanceItem.instance.scene, animation)
                        instanceItem.instance.clearTransform()
                        instanceItem.controller = ModelController.Predefined(
                            AnimationContextsFactory.create().base(),
                            AnimationItemInstanceFactory.of(animationItem),
                        )
                    } catch (ex: Throwable) {
                        logger.warn("Failed to load animation", ex)
                    }
                }
            }
        }
    }

    fun refreshAnimations() {
        scope.launch {
            ModelManagerHolder.instance.scheduleScan()
        }
    }

    fun switchCamera() {
        val cameras = PlayerRenderer.totalCameras.value?.size ?: return
        PlayerRenderer.selectedCameraIndex.getAndUpdate {
            when (it) {
                null -> 0
                cameras - 1 -> null
                else -> it + 1
            }
        }
    }

    fun setIkEnabled(index: Int, enabled: Boolean) {
        ikUpdated = true
        val instanceItem = getInstanceItem() ?: return
        instanceItem.instance.setIkEnabled(index, enabled)
    }
}
