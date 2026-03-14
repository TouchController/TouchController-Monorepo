package top.fifthlight.blazerod.runtime

import net.minecraft.client.renderer.MultiBufferSource
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.lwjgl.system.MemoryStack
import top.fifthlight.blazerod.api.refcount.AbstractRefCount
import top.fifthlight.blazerod.api.resource.ModelInstance
import top.fifthlight.blazerod.api.resource.RenderScene
import top.fifthlight.blazerod.model.NodeTransform
import top.fifthlight.blazerod.model.NodeTransformView
import top.fifthlight.blazerod.model.TransformId
import top.fifthlight.blazerod.physics.PhysicsInterface
import top.fifthlight.blazerod.physics.PhysicsScene
import top.fifthlight.blazerod.physics.PhysicsWorld
import top.fifthlight.blazerod.runtime.data.LocalMatricesBuffer
import top.fifthlight.blazerod.runtime.data.MorphTargetBuffer
import top.fifthlight.blazerod.runtime.data.RenderSkinBuffer
import top.fifthlight.blazerod.runtime.node.RenderNodeImpl
import top.fifthlight.blazerod.runtime.node.TransformMap
import top.fifthlight.blazerod.runtime.node.UpdatePhase
import top.fifthlight.blazerod.runtime.node.markNodeTransformDirty
import top.fifthlight.blazerod.runtime.resource.CameraTransformImpl
import top.fifthlight.blazerod.util.cowbuffer.CowBuffer
import top.fifthlight.blazerod.util.cowbuffer.copy
import top.fifthlight.blazerod.util.iterator.mapToArray
import top.fifthlight.mergetools.api.ActualConstructor
import top.fifthlight.mergetools.api.ActualImpl
import java.util.function.Consumer

@ActualImpl(ModelInstance::class)
class ModelInstanceImpl(
    override val scene: RenderSceneImpl,
) : AbstractRefCount(), ModelInstance {
    @ActualConstructor("of")
    constructor(scene: RenderScene) : this(scene as RenderSceneImpl)

    override val typeId: String
        get() = "model_instance"

    override var lodDistance: Float = 0f

    val modelData = ModelData(scene)
    
    internal val physicsData = if (PhysicsInterface.isPhysicsAvailable && scene.attachments[PhysicsScene::class.java] != null) {
        val physicsScene = scene.attachments[PhysicsScene::class.java] as PhysicsScene
        PhysicsData(this, scene, modelData, physicsScene)
    } else {
        null
    }

    init {
        scene.increaseReferenceCount()
        scene.attachToInstance(this)
        for (i in scene.nodes.indices) {
            updateNodeTransform(i)
        }
        if (physicsData != null) {
            top.fifthlight.blazerod.api.physics.PhysicsEngine.register(
                this,
                object : top.fifthlight.blazerod.api.physics.PhysicsProvider {
                    override fun createWorld(instance: ModelInstance): top.fifthlight.blazerod.api.physics.PhysicsWorld {
                        val bulletWorld = MemoryStack.stackPush().use { stack ->
                            val initialTransform = stack.malloc(scene.rigidBodyComponents.size * 64)
                            scene.rigidBodyComponents.forEach { (nodeIndex, component) ->
                                val nodeWorldTransform = modelData.worldTransforms[nodeIndex]
                                nodeWorldTransform.get(component.rigidBodyIndex * 64, initialTransform)
                            }
                            top.fifthlight.blazerod.physics.PhysicsWorld(physicsData.physicsScene, initialTransform)
                        }

                        return object : top.fifthlight.blazerod.api.physics.PhysicsWorld {
                            override fun applyVelocityDamping(rigidBodyIndex: Int, linearAttenuation: Float, angularAttenuation: Float) {
                                bulletWorld.applyVelocityDamping(rigidBodyIndex, linearAttenuation, angularAttenuation)
                            }
                            override fun resetRigidBody(rigidBodyIndex: Int, position: org.joml.Vector3f, rotation: org.joml.Quaternionf) {
                                bulletWorld.resetRigidBody(rigidBodyIndex, position, rotation)
                            }
                            override fun pullTransforms(dst: FloatArray) {
                                bulletWorld.pullTransforms(dst)
                            }
                            override fun pushTransforms(src: FloatArray) {
                                bulletWorld.pushTransforms(src)
                            }
                            override fun step(deltaTime: Float, maxSubSteps: Int, fixedTimeStep: Float) {
                                // Adaptive Physics Solver
                                 // If the model is close (LOD < 5m), use High-Fidelity (50 iterations)
                                 // If far away, drop to 20 iterations to save CPU.
                                 val isHighQuality = lodDistance < 5f
                                 bulletWorld.setSolverIterations(if (isHighQuality) 50 else 20)
                                
                                bulletWorld.step(deltaTime, maxSubSteps, fixedTimeStep)
                            }
                            override fun dispose() {
                                bulletWorld.close()
                            }
                        }
                    }
                }
            )
        }
        
        physicsData?.initialize()
    }

    class PhysicsData(
        val instance: ModelInstanceImpl,
        private val scene: RenderSceneImpl,
        private val modelData: ModelData,
        val physicsScene: PhysicsScene,
    ) : AutoCloseable {
        var lastPhysicsTime: Float = -1f
        var lastRootPos = org.joml.Vector3f()
        var lastFrameDistSq: Float = 0f
        
        // Speed history ring buffer for deceleration detection
        private val speedHistory = FloatArray(SPEED_HISTORY_SIZE)
        private var speedHistoryIndex = 0
        private var speedHistoryFilled = false
        
        fun recordSpeed(distSq: Float) {
            speedHistory[speedHistoryIndex] = distSq
            speedHistoryIndex = (speedHistoryIndex + 1) % SPEED_HISTORY_SIZE
            if (speedHistoryIndex == 0) speedHistoryFilled = true
        }
        
        fun averageRecentSpeed(): Float {
            val count = if (speedHistoryFilled) SPEED_HISTORY_SIZE else speedHistoryIndex
            if (count == 0) return 0f
            var sum = 0f
            for (i in 0 until count) sum += speedHistory[i]
            return sum / count
        }
        
        val world: top.fifthlight.blazerod.api.physics.PhysicsWorld
            get() = top.fifthlight.blazerod.api.physics.PhysicsEngine.getWorld(instance) ?: error("PhysicsWorld is not initialized")
        lateinit var transformArray: FloatArray
            private set

        // Adaptive throttling state
        lateinit var previousTransforms: FloatArray
            private set
        lateinit var currentTransforms: FloatArray
            private set
        var physicsAccumulator: Float = 0f
        var physicsStepTimeMs: Float = 0f
        var currentPhysicsInterval: Float = MIN_INTERVAL
        var explosionLogCount: Int = 0
        var debugStepCount: Int = 0

        companion object {
            const val BUDGET_HIGH_MS = 2.0f
            const val BUDGET_LOW_MS = 1.0f
            const val MIN_INTERVAL = 1f / 120f
            const val MAX_INTERVAL = 1f / 15f
            const val SPEED_HISTORY_SIZE = 5
            const val SPRINT_SPEED_THRESHOLD = 0.04f  // ~squared dist for sprinting speed
            const val STOP_SPEED_THRESHOLD = 0.002f   // ~squared dist for "stopped"
        }

        fun initialize() {
            // Memory is initialized when PhysicsWorld is created in the provider
            val arraySize = scene.rigidBodyComponents.size * 7
            transformArray = FloatArray(arraySize)
            previousTransforms = FloatArray(arraySize)
            currentTransforms = FloatArray(arraySize)
            world.pullTransforms(transformArray)
            transformArray.copyInto(previousTransforms)
            transformArray.copyInto(currentTransforms)
        }

        override fun close() {
        }
    }

    class ModelData(scene: RenderSceneImpl) : AutoCloseable {
        var undirtyNodeCount = 0
        var undirtyCameraCount = 0

        val transformMaps = scene.nodes.mapToArray { node ->
            TransformMap(node.absoluteTransform)
        }

        val transformDirty = Array(scene.nodes.size) { true }

        val worldTransforms = Array(scene.nodes.size) { Matrix4f() }
        val worldTransformsNoPhysics = Array(scene.nodes.size) { Matrix4f() }

        val localMatricesBuffer = run {
            val buffer = LocalMatricesBuffer(scene.primitiveComponents.size)
            buffer.clear()
            CowBuffer.acquire(buffer).also { it.increaseReferenceCount() }
        }

        val skinBuffers = scene.skins.mapIndexed { index, skin ->
            val skinBuffer = RenderSkinBuffer(skin.jointSize)
            skinBuffer.clear()
            CowBuffer.acquire(skinBuffer).also { it.increaseReferenceCount() }
        }

        val targetBuffers = scene.morphedPrimitiveComponents.mapIndexed { index, component ->
            val primitive = component.primitive
            val targets = primitive.targets!!
            val targetBuffers = MorphTargetBuffer(
                positionTargets = targets.position.targetsCount,
                colorTargets = targets.color.targetsCount,
                texCoordTargets = targets.texCoord.targetsCount,
            )
            for (targetGroup in primitive.targetGroups) {
                fun processGroup(index: Int?, channel: MorphTargetBuffer.WeightChannel, weight: Float) =
                    index?.let {
                        channel[index] = weight
                    }
                processGroup(targetGroup.position, targetBuffers.positionChannel, targetGroup.weight)
                processGroup(targetGroup.color, targetBuffers.colorChannel, targetGroup.weight)
                processGroup(targetGroup.texCoord, targetBuffers.texCoordChannel, targetGroup.weight)
            }
            CowBuffer.acquire(targetBuffers).also { it.increaseReferenceCount() }
        }

        val cameraTransforms = scene.cameras.map { CameraTransformImpl.of(it) }

        val ikEnabled = Array(scene.ikTargetData.size) { true }

        override fun close() {
            localMatricesBuffer.decreaseReferenceCount()
            skinBuffers.forEach { it.decreaseReferenceCount() }
            targetBuffers.forEach { it.decreaseReferenceCount() }
        }
    }

    override fun clearTransform() {
        modelData.undirtyNodeCount = 0
        modelData.undirtyCameraCount = 0
        for (i in scene.nodes.indices) {
            modelData.transformMaps[i].clearFrom(TransformId.ABSOLUTE.next)
            modelData.transformDirty[i] = true
        }
    }

    override fun setTransformMatrix(nodeIndex: Int, transformId: TransformId, matrix: Matrix4f) {
        markNodeTransformDirty(scene.nodes[nodeIndex])
        val transform = modelData.transformMaps[nodeIndex]
        transform.setMatrix(transformId, matrix)
    }

    override fun setTransformMatrix(
        nodeIndex: Int,
        transformId: TransformId,
        updater: Consumer<NodeTransform.Matrix>,
    ) = setTransformMatrix(nodeIndex, transformId) { updater.accept(this) }

    override fun setTransformMatrix(
        nodeIndex: Int,
        transformId: TransformId,
        updater: NodeTransform.Matrix.() -> Unit,
    ) {
        markNodeTransformDirty(scene.nodes[nodeIndex])
        val transform = modelData.transformMaps[nodeIndex]
        transform.updateMatrix(transformId, updater)
    }

    override fun setTransformDecomposed(
        nodeIndex: Int,
        transformId: TransformId,
        decomposed: NodeTransformView.Decomposed,
    ) {
        markNodeTransformDirty(scene.nodes[nodeIndex])
        val transform = modelData.transformMaps[nodeIndex]
        transform.setMatrix(transformId, decomposed)
    }

    override fun setTransformDecomposed(
        nodeIndex: Int,
        transformId: TransformId,
        updater: Consumer<NodeTransform.Decomposed>,
    ) = setTransformDecomposed(nodeIndex, transformId) { updater.accept(this) }

    override fun setTransformDecomposed(
        nodeIndex: Int,
        transformId: TransformId,
        updater: NodeTransform.Decomposed.() -> Unit,
    ) {
        markNodeTransformDirty(scene.nodes[nodeIndex])
        val transform = modelData.transformMaps[nodeIndex]
        transform.updateDecomposed(transformId, updater)
    }

    override fun setTransformBedrock(
        nodeIndex: Int,
        transformId: TransformId,
        updater: NodeTransform.Bedrock.() -> Unit,
    ) {
        markNodeTransformDirty(scene.nodes[nodeIndex])
        val transform = modelData.transformMaps[nodeIndex]
        transform.updateBedrock(transformId, updater)
    }

    override fun getIkEnabled(index: Int) = modelData.ikEnabled[index]

    override fun setIkEnabled(index: Int, enabled: Boolean) {
        val old = modelData.ikEnabled[index]
        modelData.ikEnabled[index] = enabled
        if (old && !enabled) {
            val component = scene.nodes.mapNotNull {
                it.getComponentsOfType(top.fifthlight.blazerod.runtime.node.component.RenderNodeComponent.Type.IkTarget).firstOrNull { it.ikIndex == index }
            }.firstOrNull() ?: return
            
            for (chain in component.chains) {
                val transform = modelData.transformMaps[chain.nodeIndex]
                transform.clearFrom(component.transformId)
            }
        }
    }

    override fun setGroupWeight(morphedPrimitiveIndex: Int, targetGroupIndex: Int, weight: Float) {
        val primitiveComponent = scene.morphedPrimitiveComponents[morphedPrimitiveIndex]
        val group = primitiveComponent.primitive.targetGroups[targetGroupIndex]
        val weightsIndex = requireNotNull(primitiveComponent.morphedPrimitiveIndex) {
            "Component $primitiveComponent don't have target? Check model loader"
        }
        val weights = modelData.targetBuffers[weightsIndex]
        weights.edit {
            group.position?.let { positionChannel[it] = weight }
            group.color?.let { colorChannel[it] = weight }
            group.texCoord?.let { texCoordChannel[it] = weight }
        }
    }

    override fun copyNodeWorldTransform(nodeIndex: Int, dest: Matrix4f) {
        modelData.worldTransforms[nodeIndex].get(dest)
    }

    override fun getCameraTransform(index: Int) = modelData.cameraTransforms.getOrNull(index)

    override fun debugRender(viewProjectionMatrix: Matrix4fc, bufferSource: MultiBufferSource, time: Float) {
        scene.debugRender(this, viewProjectionMatrix, bufferSource, time)
    }

    override fun updateRenderData(time: Float) {
        scene.updateRenderData(this, time)
    }

    internal fun updateNodeTransform(nodeIndex: Int) {
        val node = scene.nodes[nodeIndex]
        updateNodeTransform(node)
    }

    internal fun updateNodeTransform(node: RenderNodeImpl) {
        if (modelData.undirtyNodeCount == scene.nodes.size) {
            return
        }
        node.update(UpdatePhase.GlobalTransformPropagation, node, this)
        for (child in node.children) {
            updateNodeTransform(child)
        }
    }

    internal fun updateNodeTransformNoPhysics(node: RenderNodeImpl) {
        val nodeIndex = node.nodeIndex
        val localBase = modelData.transformMaps[nodeIndex].getSum(TransformId.EXTERNAL_PARENT_DEFORM)
        val parent = node.parent
        val dst = modelData.worldTransformsNoPhysics[nodeIndex]
        if (parent != null) {
            dst.set(modelData.worldTransformsNoPhysics[parent.nodeIndex]).mul(localBase)
        } else {
            dst.set(localBase)
        }
        for (child in node.children) {
            updateNodeTransformNoPhysics(child)
        }
    }

    internal fun updateWorldTransformsNoPhysics() {
        updateNodeTransformNoPhysics(scene.rootNode)
    }

    override fun createRenderTask(
        modelMatrix: Matrix4fc,
        light: Int,
        overlay: Int,
    ): RenderTaskImpl {
        return RenderTaskImpl.acquire(
            instance = this,
            modelMatrix = modelMatrix,
            light = light,
            overlay = overlay,
            localMatricesBuffer = modelData.localMatricesBuffer.copy(),
            skinBuffer = modelData.skinBuffers.copy(),
            morphTargetBuffer = modelData.targetBuffers.copy().also { buffer ->
                buffer.forEach {
                    it.content.uploadIndices()
                }
            },
        ).apply {
            scene.renderTransform?.matrix?.let {
                this.modelMatrix.mul(it)
            }
        }
    }

    override fun onClosed() {
        top.fifthlight.blazerod.api.physics.PhysicsEngine.unregister(this)
        scene.decreaseReferenceCount()
        physicsData?.close()
        modelData.close()
    }
}
