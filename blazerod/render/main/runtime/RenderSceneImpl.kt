package top.fifthlight.blazerod.runtime

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap
import net.minecraft.client.renderer.MultiBufferSource
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.blazerod.api.refcount.AbstractRefCount
import top.fifthlight.blazerod.api.resource.RenderExpression
import top.fifthlight.blazerod.api.resource.RenderExpressionGroup
import top.fifthlight.blazerod.api.resource.RenderScene
import top.fifthlight.blazerod.model.Camera
import top.fifthlight.blazerod.model.HumanoidTag
import top.fifthlight.blazerod.model.NodeId
import top.fifthlight.blazerod.model.NodeTransform
import top.fifthlight.blazerod.model.RigidBody
import top.fifthlight.blazerod.physics.PhysicsInterface
import top.fifthlight.blazerod.physics.PhysicsScene
import top.fifthlight.blazerod.runtime.node.RenderNodeImpl
import top.fifthlight.blazerod.runtime.node.UpdatePhase
import kotlin.math.sqrt
import top.fifthlight.blazerod.runtime.node.component.IkTargetComponent
import top.fifthlight.blazerod.runtime.node.component.PrimitiveComponent
import top.fifthlight.blazerod.runtime.node.component.RenderNodeComponent
import top.fifthlight.blazerod.runtime.node.component.RigidBodyComponent
import top.fifthlight.blazerod.runtime.node.forEach
import top.fifthlight.blazerod.runtime.resource.RenderPhysicsJoint
import top.fifthlight.blazerod.runtime.resource.RenderSkin
import org.slf4j.LoggerFactory

import kotlin.time.measureTime

class RenderSceneImpl(
    override val rootNode: RenderNodeImpl,
    override val nodes: List<RenderNodeImpl>,
    val skins: List<RenderSkin>,
    override val expressions: List<RenderExpression>,
    override val expressionGroups: List<RenderExpressionGroup>,
    override val cameras: List<Camera>,
    val physicsJoints: List<RenderPhysicsJoint>,
    override val renderTransform: NodeTransform?,
) : AbstractRefCount(), RenderScene {
    override val attachments: Map<Class<*>, Any>
    companion object {
        private val logger = LoggerFactory.getLogger(RenderSceneImpl::class.java)
        private const val PHYSICS_MAX_SUB_STEP_COUNT = 4
        private const val PHYSICS_FPS = 120f
        private const val PHYSICS_TIME_STEP = 1f / PHYSICS_FPS
        private const val PHYSICS_ANOMALY_DISTANCE = 8.0f
        private const val PHYSICS_POSE_JUMP_DISTANCE = 0.35f
    }

    override val typeId: String
        get() = "scene"

    private val sortedNodes: List<RenderNodeImpl>
    private val debugRenderNodes: List<RenderNodeImpl>
    val primitiveComponents: List<PrimitiveComponent>
    val morphedPrimitiveComponents: List<PrimitiveComponent>
    override val ikTargetData: List<RenderScene.IkTargetData>
    val ikTargetComponents: List<IkTargetComponent>
    val rigidBodyComponents: List<Pair<Int, RigidBodyComponent>>
    override val nodeIdMap: Map<NodeId, RenderNodeImpl>
    override val nodeNameMap: Map<String, RenderNodeImpl>
    override val humanoidTagMap: Map<HumanoidTag, RenderNodeImpl>
    val physicsScene: PhysicsScene?

    init {
        rootNode.increaseReferenceCount()
        val nodes = mutableListOf<RenderNodeImpl>()
        val debugRenderNodes = mutableListOf<RenderNodeImpl>()
        val primitiveComponents = mutableListOf<PrimitiveComponent>()
        val morphedPrimitives = Int2ReferenceOpenHashMap<PrimitiveComponent>()
        val ikTargets = Int2ReferenceOpenHashMap<IkTargetComponent>()
        val rigidBodyComponents = Int2ReferenceOpenHashMap<Pair<Int, RigidBodyComponent>>()
        val nodeIdMap = mutableMapOf<NodeId, RenderNodeImpl>()
        val nodeNameMap = mutableMapOf<String, RenderNodeImpl>()
        val humanoidTagMap = mutableMapOf<HumanoidTag, RenderNodeImpl>()
        rootNode.forEach { node ->
            nodes.add(node)
            node.nodeId?.let { nodeIdMap.put(it, node) }
            node.nodeName?.let { nodeNameMap.put(it, node) }
            node.humanoidTags.forEach { humanoidTagMap[it] = node }
            if (node.hasPhase(UpdatePhase.Type.DEBUG_RENDER)) {
                debugRenderNodes.add(node)
            }
            node.getComponentsOfType(RenderNodeComponent.Type.Primitive).let { components ->
                primitiveComponents.addAll(components)
                for (component in components) {
                    component.morphedPrimitiveIndex?.let { index ->
                        if (morphedPrimitives.containsKey(index)) {
                            throw IllegalStateException("Duplicate morphed primitive index: $index")
                        }
                        morphedPrimitives.put(index, component)
                    }
                }
            }
            node.getComponentsOfType(RenderNodeComponent.Type.IkTarget).forEach { component ->
                ikTargets.put(component.ikIndex, component)
            }
            node.getComponentsOfType(RenderNodeComponent.Type.RigidBody).forEach { component ->
                rigidBodyComponents.put(component.rigidBodyIndex, node.nodeIndex to component)
            }
        }
        this.sortedNodes = nodes
        this.debugRenderNodes = debugRenderNodes
        this.primitiveComponents = primitiveComponents
        this.morphedPrimitiveComponents = (0 until morphedPrimitives.size).map {
            morphedPrimitives.get(it) ?: error("Morphed primitive index not found: $it")
        }
        this.ikTargetData = (0 until ikTargets.size).map {
            val ikTarget = ikTargets.get(it) ?: error("Ik target index not found: $it")
            RenderScene.IkTargetData(nodes[ikTarget.effectorNodeIndex])
        }
        this.ikTargetComponents = (0 until ikTargets.size).map {
            ikTargets.get(it) ?: error("Ik target index not found: $it")
        }
        this.rigidBodyComponents = (0 until rigidBodyComponents.size).map {
            rigidBodyComponents.get(it) ?: error("Rigid body index not found: $it")
        }
        this.nodeIdMap = nodeIdMap
        this.nodeNameMap = nodeNameMap
        this.humanoidTagMap = humanoidTagMap
        
        val attachmentsMap = mutableMapOf<Class<*>, Any>()
        this.physicsScene = this.rigidBodyComponents.takeIf {
            PhysicsInterface.isPhysicsAvailable && it.isNotEmpty()
        }?.let { components ->
            PhysicsScene(
                rigidBodies = components.map { (nodeIndex, component) -> component.rigidBodyData },
                joints = physicsJoints,
            ).also { scene ->
                attachmentsMap[PhysicsScene::class.java] = scene
            }
        }
        this.attachments = attachmentsMap
    }

    private fun executePhase(instance: ModelInstanceImpl, phase: UpdatePhase) {
        for (node in sortedNodes) {
            node.update(phase, node, instance)
        }
    }

    fun resetPhysics(instance: ModelInstanceImpl, time: Float) {
        val data = instance.physicsData ?: return

        instance.updateWorldTransformsNoPhysics()
        resetRigidBodiesToCurrentPose(instance, data)
        data.lastPhysicsTime = time
        data.physicsAccumulator = 0f
        data.physicsStepTimeMs = 0f
        data.currentPhysicsInterval = ModelInstanceImpl.PhysicsData.MIN_INTERVAL

        executePhase(instance, UpdatePhase.PhysicsUpdatePost)
        executePhase(instance, UpdatePhase.GlobalTransformPropagation)
        logPhysicsAnomalies(instance, data, "reset")
    }

    private fun resetRigidBodiesToCurrentPose(
        instance: ModelInstanceImpl,
        data: ModelInstanceImpl.PhysicsData,
    ) {
        val position = Vector3f()
        val rotation = Quaternionf()
        for ((nodeIndex, component) in rigidBodyComponents) {
            val nodeWorld = instance.modelData.worldTransformsNoPhysics[nodeIndex]
            nodeWorld.getTranslation(position)
            nodeWorld.getUnnormalizedRotation(rotation)
            data.world.resetRigidBody(component.rigidBodyIndex, position, rotation)
        }

        executePhase(instance, UpdatePhase.PhysicsUpdatePre)
        data.world.pullTransforms(data.transformArray)
        data.transformArray.copyInto(data.previousTransforms)
        data.transformArray.copyInto(data.currentTransforms)
    }

    private fun hasAbruptKinematicPoseChange(data: ModelInstanceImpl.PhysicsData): Boolean {
        val thresholdSq = PHYSICS_POSE_JUMP_DISTANCE * PHYSICS_POSE_JUMP_DISTANCE
        for ((_, component) in rigidBodyComponents) {
            if (component.rigidBodyData.physicsMode == RigidBody.PhysicsMode.PHYSICS) {
                continue
            }

            val offset = component.rigidBodyIndex * 7
            val dx = data.transformArray[offset] - data.currentTransforms[offset]
            val dy = data.transformArray[offset + 1] - data.currentTransforms[offset + 1]
            val dz = data.transformArray[offset + 2] - data.currentTransforms[offset + 2]
            if (dx * dx + dy * dy + dz * dz > thresholdSq) {
                return true
            }
        }
        return false
    }



    private fun updatePhysics(
        instance: ModelInstanceImpl,
        time: Float,
    ) {
        val distance = instance.lodDistance
        if (distance > 64f) {
            return
        }

        instance.physicsData?.let { data ->
            if (data.lastPhysicsTime < 0) {
                data.lastPhysicsTime = time

                instance.updateWorldTransformsNoPhysics()
                resetRigidBodiesToCurrentPose(instance, data)

                return@let
            }

            val timeStep = time - data.lastPhysicsTime
            if (timeStep <= 0f) {
                return@let
            }
            data.lastPhysicsTime = time

            val distanceFpsMultiplier = when {
                distance < 16f -> 1f
                distance < 32f -> 0.5f
                else -> 0.25f
            }

            // --- Adaptive throttling ---
            val minInterval = ModelInstanceImpl.PhysicsData.MIN_INTERVAL / distanceFpsMultiplier
            val effectiveInterval = maxOf(data.currentPhysicsInterval, minInterval)
            val maxAccumulator = effectiveInterval * 2f
            data.physicsAccumulator = minOf(data.physicsAccumulator + timeStep, maxAccumulator)

            if (data.physicsAccumulator >= effectiveInterval) {
                data.currentTransforms.copyInto(data.previousTransforms)

                instance.updateWorldTransformsNoPhysics()
                executePhase(instance, UpdatePhase.PhysicsUpdatePre)

                if (hasAbruptKinematicPoseChange(data)) {
                    resetRigidBodiesToCurrentPose(instance, data)
                }

                data.world.pushTransforms(data.transformArray)

                val stepStart = System.nanoTime()
                // Adaptive throttling can wait longer than four 120 Hz steps. Widen the
                // fixed step so Bullet consumes the full elapsed interval instead of
                // leaving dynamic bones progressively behind the animated skeleton.
                val fixedTimeStep = maxOf(
                    PHYSICS_TIME_STEP,
                    data.physicsAccumulator / PHYSICS_MAX_SUB_STEP_COUNT,
                )
                data.world.step(data.physicsAccumulator, PHYSICS_MAX_SUB_STEP_COUNT, fixedTimeStep)
                val stepTimeMs = (System.nanoTime() - stepStart) / 1_000_000f
                data.debugStepCount++

                data.world.pullTransforms(data.transformArray)
                if (hasInvalidTransforms(data)) {
                    logPhysicsAnomalies(instance, data, "step")
                    logger.warn(
                        "[PMX-PHYSICS-RUNTIME] recreating physics world after non-finite step output count={} time={}",
                        data.debugStepCount,
                        time,
                    )
                    data.recreateWorldFromCurrentPose()
                    resetPhysics(instance, time)
                    return@let
                }
                data.transformArray.copyInto(data.currentTransforms)
                data.physicsAccumulator = 0f

                // Adapt physics rate based on step cost (EMA with hysteresis)
                data.physicsStepTimeMs = 0.8f * data.physicsStepTimeMs + 0.2f * stepTimeMs
                if (data.physicsStepTimeMs > ModelInstanceImpl.PhysicsData.BUDGET_HIGH_MS) {
                    data.currentPhysicsInterval = minOf(
                        data.currentPhysicsInterval * 2f,
                        ModelInstanceImpl.PhysicsData.MAX_INTERVAL
                    )
                } else if (data.physicsStepTimeMs < ModelInstanceImpl.PhysicsData.BUDGET_LOW_MS) {
                    data.currentPhysicsInterval = maxOf(
                        data.currentPhysicsInterval / 2f,
                        ModelInstanceImpl.PhysicsData.MIN_INTERVAL
                    )
                }

                executePhase(instance, UpdatePhase.PhysicsUpdatePost)
                executePhase(instance, UpdatePhase.GlobalTransformPropagation)
            } else {
                val alpha = data.physicsAccumulator / effectiveInterval
                PhysicsTransformUtil.interpolate(
                    data.previousTransforms, data.currentTransforms, data.transformArray,
                    rigidBodyComponents.size, alpha
                )
                if (hasInvalidTransforms(data)) {
                    logger.warn(
                        "[PMX-PHYSICS-RUNTIME] resetting physics after invalid interpolated transform time={}",
                        time,
                    )
                    resetPhysics(instance, time)
                    return@let
                }

                executePhase(instance, UpdatePhase.PhysicsUpdatePost)
                executePhase(instance, UpdatePhase.GlobalTransformPropagation)
            }
        }
    }

    fun debugRender(
        instance: ModelInstanceImpl,
        viewProjectionMatrix: Matrix4fc,
        bufferSource: MultiBufferSource,
        time: Float,
    ) {
        if (debugRenderNodes.isEmpty()) {
            return
        }
        if (instance.modelData.undirtyNodeCount != nodes.size) {
            executePhase(instance, UpdatePhase.GlobalTransformPropagation)
            executePhase(instance, UpdatePhase.IkUpdate)
            executePhase(instance, UpdatePhase.InfluenceTransformUpdate)
            executePhase(instance, UpdatePhase.GlobalTransformPropagation)
            if (instance.physicsData != null) {
                updatePhysics(instance, time)
                executePhase(instance, UpdatePhase.GlobalTransformPropagation)
            }
        } else if (instance.physicsData != null) {
            executePhase(instance, UpdatePhase.GlobalTransformPropagation)
            updatePhysics(instance, time)
        }
        UpdatePhase.DebugRender.acquire(viewProjectionMatrix, bufferSource).use {
            executePhase(instance, it)
        }
    }

    private fun logPhysicsAnomalies(
        instance: ModelInstanceImpl,
        data: ModelInstanceImpl.PhysicsData,
        phase: String,
    ) {
        var nonFinite = 0
        var farCount = 0
        val samples = mutableListOf<String>()
        val noPhysicsPos = Vector3f()
        for ((nodeIndex, component) in rigidBodyComponents) {
            val offset = component.rigidBodyIndex * 7
            val px = data.transformArray[offset + 0]
            val py = data.transformArray[offset + 1]
            val pz = data.transformArray[offset + 2]
            if (!px.isFinite() || !py.isFinite() || !pz.isFinite()) {
                nonFinite++
                if (samples.size < 3) {
                    samples.add("body=${component.rigidBodyIndex} node=${nodes[nodeIndex].nodeName} rb=${component.rigidBodyData.name} mode=${component.rigidBodyData.physicsMode} pos=($px,$py,$pz)")
                }
                continue
            }
            instance.modelData.worldTransformsNoPhysics[nodeIndex].getTranslation(noPhysicsPos)
            val dx = px - noPhysicsPos.x
            val dy = py - noPhysicsPos.y
            val dz = pz - noPhysicsPos.z
            val dist = sqrt(dx * dx + dy * dy + dz * dz)
            if (dist > PHYSICS_ANOMALY_DISTANCE) {
                farCount++
                if (samples.size < 3) {
                    samples.add("body=${component.rigidBodyIndex} node=${nodes[nodeIndex].nodeName} rb=${component.rigidBodyData.name} mode=${component.rigidBodyData.physicsMode} dist=$dist pos=($px,$py,$pz) bone=(${noPhysicsPos.x},${noPhysicsPos.y},${noPhysicsPos.z})")
                }
            }
        }
        if (nonFinite > 0 || farCount > 0) {
            logger.warn(
                "[PMX-PHYSICS-RUNTIME] anomaly phase={} nonFinite={} farBodies={} threshold={} samples={}",
                phase,
                nonFinite,
                farCount,
                PHYSICS_ANOMALY_DISTANCE,
                samples.joinToString("; "),
            )
        }
    }

    private fun hasInvalidTransforms(data: ModelInstanceImpl.PhysicsData): Boolean {
        val count = rigidBodyComponents.size
        for (i in 0 until count) {
            val offset = i * 7
            if (!PhysicsTransformUtil.isValidTransform(data.transformArray, offset)) {
                return true
            }
        }
        return false
    }

    private fun updateRenderData(instance: ModelInstanceImpl, time: Float, allowPhysics: Boolean) {
        if (instance.modelData.undirtyNodeCount != nodes.size) {
            executePhase(instance, UpdatePhase.GlobalTransformPropagation)
            executePhase(instance, UpdatePhase.IkUpdate)
            executePhase(instance, UpdatePhase.InfluenceTransformUpdate)
            executePhase(instance, UpdatePhase.GlobalTransformPropagation)
            if (allowPhysics && instance.physicsData != null) {
                updatePhysics(instance, time)
                executePhase(instance, UpdatePhase.GlobalTransformPropagation)
            }
            executePhase(instance, UpdatePhase.RenderDataUpdate)
            executePhase(instance, UpdatePhase.CameraUpdate)
        } else if (allowPhysics && instance.physicsData != null) {
            executePhase(instance, UpdatePhase.GlobalTransformPropagation)
            updatePhysics(instance, time)
            executePhase(instance, UpdatePhase.RenderDataUpdate)
        }
    }

    fun updateRenderData(instance: ModelInstanceImpl, time: Float) {
        updateRenderData(instance, time, allowPhysics = true)
    }

    fun updateRenderDataNoPhysics(instance: ModelInstanceImpl, time: Float) {
        updateRenderData(instance, time, allowPhysics = false)
    }

    internal fun attachToInstance(instance: ModelInstanceImpl) {
        executePhase(instance, UpdatePhase.GlobalTransformPropagation)
        executePhase(instance, UpdatePhase.IkUpdate)
        executePhase(instance, UpdatePhase.InfluenceTransformUpdate)
        executePhase(instance, UpdatePhase.GlobalTransformPropagation)
        for (node in nodes) {
            for (component in node.components) {
                component.onAttached(instance, node)
            }
        }
    }

    override fun onClosed() {
        rootNode.decreaseReferenceCount()
        physicsScene?.close()
    }
}
