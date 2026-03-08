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
        private const val PHYSICS_MAX_SUB_STEP_COUNT = 4
        private const val PHYSICS_FPS = 120f
        private const val PHYSICS_TIME_STEP = 1f / PHYSICS_FPS
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



    /**
     * Interpolates between previous and current transform arrays using nlerp for rotations.
     * Each rigid body has 7 floats: [px, py, pz, qx, qy, qz, qw].
     */
    private fun interpolateTransforms(
        prev: FloatArray, curr: FloatArray, dst: FloatArray,
        count: Int, alpha: Float
    ) {
        for (i in 0 until count) {
            val o = i * 7
            // Position: linear interpolation
            dst[o + 0] = prev[o + 0] + (curr[o + 0] - prev[o + 0]) * alpha
            dst[o + 1] = prev[o + 1] + (curr[o + 1] - prev[o + 1]) * alpha
            dst[o + 2] = prev[o + 2] + (curr[o + 2] - prev[o + 2]) * alpha
            // Rotation: nlerp (normalized linear interpolation, cheaper than slerp)
            var qx = prev[o + 3] + (curr[o + 3] - prev[o + 3]) * alpha
            var qy = prev[o + 4] + (curr[o + 4] - prev[o + 4]) * alpha
            var qz = prev[o + 5] + (curr[o + 5] - prev[o + 5]) * alpha
            var qw = prev[o + 6] + (curr[o + 6] - prev[o + 6]) * alpha
            val invLen = 1f / sqrt(qx * qx + qy * qy + qz * qz + qw * qw)
            dst[o + 3] = qx * invLen
            dst[o + 4] = qy * invLen
            dst[o + 5] = qz * invLen
            dst[o + 6] = qw * invLen
        }
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
                executePhase(instance, UpdatePhase.PhysicsUpdatePre)
                data.world.pushTransforms(data.transformArray)

                val initPos = Vector3f()
                val initRot = Quaternionf()
                for ((nodeIndex, component) in rigidBodyComponents) {
                    val nodeWorld = instance.modelData.worldTransforms[nodeIndex]
                    nodeWorld.getTranslation(initPos)
                    nodeWorld.getUnnormalizedRotation(initRot)
                    data.world.resetRigidBody(component.rigidBodyIndex, initPos, initRot)
                }
                
                instance.modelData.worldTransformsNoPhysics[rootNode.nodeIndex].getTranslation(data.lastRootPos)

                data.world.pullTransforms(data.transformArray)
                data.transformArray.copyInto(data.previousTransforms)
                data.transformArray.copyInto(data.currentTransforms)

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
            // Safety: Never accumulate more than 0.5 seconds of physics time.
            data.physicsAccumulator = minOf(data.physicsAccumulator + timeStep, 0.5f)

            if (data.physicsAccumulator >= effectiveInterval) {
                data.currentTransforms.copyInto(data.previousTransforms)

                instance.updateWorldTransformsNoPhysics()
                executePhase(instance, UpdatePhase.PhysicsUpdatePre)
                
                val rootPos = Vector3f()
                instance.modelData.worldTransformsNoPhysics[rootNode.nodeIndex].getTranslation(rootPos)
                val distSq = rootPos.distanceSquared(data.lastRootPos)
                
                val avgSpeed = data.averageRecentSpeed()
                if (distSq > 4.0f || (avgSpeed > ModelInstanceImpl.PhysicsData.SPRINT_SPEED_THRESHOLD && distSq < ModelInstanceImpl.PhysicsData.STOP_SPEED_THRESHOLD * 2f)) {
                    val initPos = Vector3f()
                    val initRot = Quaternionf()
                    for ((nodeIndex, component) in rigidBodyComponents) {
                        val nodeWorld = instance.modelData.worldTransformsNoPhysics[nodeIndex]
                        nodeWorld.getTranslation(initPos)
                        nodeWorld.getUnnormalizedRotation(initRot)
                        data.world.resetRigidBody(component.rigidBodyIndex, initPos, initRot)
                    }
                    executePhase(instance, UpdatePhase.PhysicsUpdatePre)
                }
                
                data.recordSpeed(distSq)
                data.lastFrameDistSq = distSq
                data.lastRootPos.set(rootPos)

                data.world.pushTransforms(data.transformArray)

                val stepStart = System.nanoTime()
                data.world.step(data.physicsAccumulator, PHYSICS_MAX_SUB_STEP_COUNT, PHYSICS_TIME_STEP)
                val stepTimeMs = (System.nanoTime() - stepStart) / 1_000_000f

                data.world.pullTransforms(data.transformArray)
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
                interpolateTransforms(
                    data.previousTransforms, data.currentTransforms, data.transformArray,
                    rigidBodyComponents.size, alpha
                )

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

    fun updateRenderData(instance: ModelInstanceImpl, time: Float) {
        if (instance.modelData.undirtyNodeCount != nodes.size) {
            executePhase(instance, UpdatePhase.GlobalTransformPropagation)
            executePhase(instance, UpdatePhase.IkUpdate)
            executePhase(instance, UpdatePhase.InfluenceTransformUpdate)
            executePhase(instance, UpdatePhase.GlobalTransformPropagation)
            if (instance.physicsData != null) {
                updatePhysics(instance, time)
                executePhase(instance, UpdatePhase.GlobalTransformPropagation)
            }
            executePhase(instance, UpdatePhase.RenderDataUpdate)
            executePhase(instance, UpdatePhase.CameraUpdate)
        } else if (instance.physicsData != null) {
            executePhase(instance, UpdatePhase.GlobalTransformPropagation)
            updatePhysics(instance, time)
            executePhase(instance, UpdatePhase.RenderDataUpdate)
        }
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
