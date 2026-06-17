package top.fifthlight.blazerod.api.physics

import top.fifthlight.blazerod.api.resource.ModelInstance

object PhysicsEngine {
    private val activeWorlds = mutableMapOf<ModelInstance, PhysicsWorld>()
    private val providers = mutableMapOf<ModelInstance, PhysicsProvider>()
    
    fun register(instance: ModelInstance, provider: PhysicsProvider) {
        providers[instance] = provider
        if (!activeWorlds.containsKey(instance)) {
            activeWorlds[instance] = provider.createWorld(instance)
        }
    }

    fun unregister(instance: ModelInstance) {
        providers.remove(instance)
        activeWorlds.remove(instance)?.dispose()
    }

    fun getWorld(instance: ModelInstance): PhysicsWorld? {
        return activeWorlds[instance]
    }

    fun recreate(instance: ModelInstance): PhysicsWorld? {
        val provider = providers[instance] ?: return null
        activeWorlds.remove(instance)?.dispose()
        val world = provider.createWorld(instance)
        activeWorlds[instance] = world
        return world
    }

    fun update(time: Float) {
        val iterator = activeWorlds.iterator()
        while (iterator.hasNext()) {
            val (instance, world) = iterator.next()
            if (instance.referenceCount <= 0) {
                world.dispose()
                iterator.remove()
            }
        }
    }
}

interface PhysicsWorld {
    fun applyVelocityDamping(rigidBodyIndex: Int, linearAttenuation: Float, angularAttenuation: Float)
    fun resetRigidBody(rigidBodyIndex: Int, position: org.joml.Vector3f, rotation: org.joml.Quaternionf)
    fun pullTransforms(dst: FloatArray)
    fun pushTransforms(src: FloatArray)
    fun step(deltaTime: Float, maxSubSteps: Int, fixedTimeStep: Float)
    fun dispose()
}

interface PhysicsProvider {
    fun createWorld(instance: ModelInstance): PhysicsWorld
}
