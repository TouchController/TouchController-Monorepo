package top.fifthlight.blazerod.runtime.node

import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.blazerod.model.NodeTransform
import top.fifthlight.blazerod.model.NodeTransformView
import top.fifthlight.blazerod.model.TransformId
import java.util.*

class TransformMap(first: NodeTransformView?) {
    val transforms = EnumMap<TransformId, NodeTransform>(TransformId::class.java).also {
        it[TransformId.FIRST] = first?.clone() ?: NodeTransform.Decomposed()
    }

    private val dirtyTransforms = EnumSet.noneOf(TransformId::class.java)

    private val intermediateMatrices = EnumMap<TransformId, Matrix4f>(TransformId::class.java).also {
        it[TransformId.FIRST] = Matrix4f().also { matrix -> first?.applyOnMatrix(matrix) }
    }

    /**
     * 清除从指定 TransformId 开始的所有变换。
     *
     * @param id 起始 TransformId。
     */
    fun clearFrom(id: TransformId = TransformId.FIRST) {
        transforms.keys.removeIf { it >= id }
        intermediateMatrices.keys.removeIf { it >= id }
        dirtyTransforms.removeIf { it >= id }
    }

    private val tempAccumulatedMatrix = Matrix4f()
    private fun calculateIntermediateMatrices(targetId: TransformId): Matrix4fc {
        // 1. 确定计算的起始点 (startId)。
        // 找到从 targetId 向前追溯，第一个非脏并存在的 TransformId。
        // 如果所有祖先都脏，则从 TransformId.FIRST 开始。
        var startId = targetId

        while (startId in dirtyTransforms || startId !in transforms.keys) {
            if (startId == TransformId.FIRST) {
                break // 如果 FIRST 都脏，就从 FIRST 自身开始
            }

            startId = TransformId.entries[startId.ordinal - 1]
        }

        // 获取起始点的基础矩阵。
        // 如果 startId 是 FIRST 且它自身被标记为脏（意味着需要重新初始化其矩阵），则从原始 transform 获取。
        // 否则，从 intermediateMatrices 获取。
        val baseMatrix = if (startId == TransformId.FIRST && dirtyTransforms.contains(TransformId.FIRST)) {
            transforms[TransformId.FIRST]!!.setOnMatrix(tempAccumulatedMatrix)
        } else {
            tempAccumulatedMatrix.set(
                intermediateMatrices[startId]
                    ?: error("Base matrix for $startId not found and not dirty, this should not happen.")
            )
        }

        // 将基础矩阵复制到可重用的临时矩阵中，作为累积的起点。
        tempAccumulatedMatrix.set(baseMatrix)

        // 2. 从起始点的下一个 ID 开始，逐步累积到 targetId。
        for (i in (startId.ordinal + 1)..targetId.ordinal) {
            val currentId = TransformId.entries[i]

            // 获取当前 ID 对应的 NodeTransform。
            val transform = transforms[currentId]
            if (transform == null) {
                dirtyTransforms.remove(currentId)
                continue // 跳过当前迭代
            }

            // 将当前变换矩阵与累积矩阵相乘。
            transform.applyOnMatrix(tempAccumulatedMatrix)

            // 存储当前的累积矩阵到缓存中，并标记为不脏。
            val matrixToUpdate = intermediateMatrices.getOrPut(currentId) { Matrix4f() }
            matrixToUpdate.set(tempAccumulatedMatrix)
            dirtyTransforms.remove(currentId)
        }

        return tempAccumulatedMatrix
    }

    /**
     * 获取指定 TransformId 对应的 NodeTransformView。
     *
     * 注意：为了避免额外对象分配开销，这个方法返回的矩阵持有对内部数据的引用，因此在数据发生变更时返回的矩阵会被修改，
     * 也就是说，返回值只在 TransformMap 被其他方法修改前有效。
     *
     * @param id 要获取的 TransformId。
     * @return 对应的 NodeTransformView，如果不存在则为 null。
     */
    fun get(id: TransformId): NodeTransformView? = transforms[id]

    /**
     * 获取指定 TransformId 的累积变换矩阵。
     * 如果该 ID 或其任何祖先被标记为脏，则会重新计算。
     *
     * 注意：为了避免额外对象分配开销，这个方法返回的矩阵持有对内部数据的引用，因此在数据发生变更时返回的矩阵会被修改，
     * 也就是说，返回值只在 TransformMap 被其他方法修改前有效。
     *
     * @param id 目标 TransformId。
     * @return 累积变换矩阵。
     */
    fun getSum(id: TransformId): Matrix4fc {
        // 如果 id 本身是脏的，或者它的任何一个祖先是脏的，我们需要重新计算。
        // calculateIntermediateMatrices 会自动处理从最近非脏点开始计算的逻辑。
        return if (dirtyTransforms.contains(id)) {
            calculateIntermediateMatrices(id)
        } else {
            // 如果 id 不脏，直接返回上一级存在的缓存矩阵。
            for (i in (0..id.ordinal).reversed()) {
                intermediateMatrices[TransformId.entries[i]]?.let { return it }
            }
            throw IllegalStateException("There must be a intermediate matrix for ${TransformId.entries.first()}.")
        }
    }

    // 标记当前 ID 及其后续所有 ID 为脏
    private fun markDirty(id: TransformId) {
        for (i in id.ordinal until TransformId.entries.size) {
            dirtyTransforms.add(TransformId.entries[i])
        }
    }

    /**
     * 更新指定 TransformId 的 NodeTransform，并确保其为 Decomposed 类型。
     * 如果当前存储的变换不是 Decomposed 类型，它将被转换为 Decomposed 类型。
     * 如果该 ID 不存在，则会创建一个新的 NodeTransform.Decomposed 并插入。
     *
     * @param id 要更新的 TransformId。
     * @param updater 用于修改 NodeTransform.Decomposed 的 lambda 表达式。
     */
    fun updateDecomposed(id: TransformId, updater: NodeTransform.Decomposed.() -> Unit) {
        val currentTransform = transforms[id]
        val targetTransform: NodeTransform.Decomposed

        if (currentTransform is NodeTransform.Decomposed) {
            targetTransform = currentTransform
        } else {
            // 如果不存在或类型不匹配，则从当前矩阵或默认值创建新的 Decomposed 变换。
            // 尝试从现有矩阵中提取平移、旋转、缩放信息，否则使用默认值。
            targetTransform = NodeTransform.Decomposed(
                translation = currentTransform?.getTranslation(Vector3f()) ?: Vector3f(),
                rotation = currentTransform?.getRotation(Quaternionf()) ?: Quaternionf(),
                scale = currentTransform?.getScale(Vector3f()) ?: Vector3f(1f)
            )
            transforms[id] = targetTransform // 替换旧的变换
            intermediateMatrices.getOrPut(id, ::Matrix4f)
        }

        updater(targetTransform) // 应用更新
        markDirty(id) // 标记为脏
    }

    /**
     * 更新指定 TransformId 的 NodeTransform，并确保其为 Matrix 类型。
     * 如果当前存储的变换不是 Matrix 类型，它将被转换为 Matrix 类型。
     * 如果该 ID 不存在，则会创建一个新的 NodeTransform.Matrix 并插入。
     *
     * @param id 要更新的 TransformId。
     * @param updater 用于修改 NodeTransform.Matrix 的 lambda 表达式。
     */
    fun updateMatrix(id: TransformId, updater: NodeTransform.Matrix.() -> Unit) {
        val currentTransform = transforms[id]
        val targetTransform: NodeTransform.Matrix

        if (currentTransform is NodeTransform.Matrix) {
            targetTransform = currentTransform
        } else {
            // 如果不存在或类型不匹配，则从当前矩阵或默认值创建新的 Matrix 变换。
            // 如果没有现有矩阵，则使用单位矩阵。
            targetTransform = NodeTransform.Matrix().apply {
                currentTransform?.setOnMatrix(matrix)
            }
            transforms[id] = targetTransform // 替换旧的变换
            intermediateMatrices.getOrPut(id, ::Matrix4f)
        }

        updater(targetTransform) // 应用更新
        markDirty(id) // 标记为脏
    }

    /**
     * 批量更新指定 TransformId 的 NodeTransform，并确保其为 Bedrock 类型。
     * 如果当前存储的变换不是 Bedrock 类型，它将被转换为 Bedrock 类型。
     * 如果该 ID 不存在，则会创建一个新的 NodeTransform.Bedrock 并插入。
     *
     * @param id 要更新的 TransformId。
     * @param updater 用于修改 NodeTransform.Bedrock 的 lambda 表达式。
     */
    fun updateBedrock(id: TransformId, updater: NodeTransform.Bedrock.() -> Unit) {
        val current = transforms[id]
        val bedrock = if (current is NodeTransform.Bedrock) {
            current
        } else {
            val pivot = (transforms[TransformId.ABSOLUTE] as? NodeTransform.Bedrock)?.pivot
            val new = NodeTransform.Bedrock(
                pivot = pivot ?: Vector3f(),
                rotation = Quaternionf(),
                translation = Vector3f(),
                scale = Vector3f(1f),
            )
            transforms[id] = new
            intermediateMatrices.getOrPut(id) { Matrix4f() }
            new
        }
        updater(bedrock)
        markDirty(id)
    }

    /**
     * 直接设置指定 TransformId 的变换矩阵。
     * 如果当前存储的变换不是 Matrix 类型，它将被转换为 Matrix 类型。
     * 如果该 ID 不存在，则会创建一个新的 NodeTransform.Matrix 并插入。
     *
     * @param id 要设置的 TransformId。
     * @param matrix 要设置的 Matrix4fc 实例。
     */
    fun setMatrix(id: TransformId, matrix: Matrix4fc) {
        val currentTransform = transforms[id]
        val targetTransform: NodeTransform.Matrix

        if (currentTransform is NodeTransform.Matrix) {
            targetTransform = currentTransform
        } else {
            // 如果不存在或类型不匹配，则创建新的 Matrix 变换。
            targetTransform = NodeTransform.Matrix()
            transforms[id] = targetTransform // 替换旧的变换
            intermediateMatrices.getOrPut(id, ::Matrix4f)
        }

        targetTransform.matrix.set(matrix) // 设置新的矩阵
        markDirty(id) // 标记为脏
    }

    /**
     * 直接设置指定 TransformId 的变换矩阵。
     * 如果当前存储的变换不是 Decomposed 类型，它将被转换为 Decomposed 类型。
     * 如果该 ID 不存在，则会创建一个新的 NodeTransform.Decomposed 并插入。
     *
     * @param id 要设置的 TransformId。
     * @param decomposed 要设置的 NodeTransform.Decomposed 实例。
     */
    fun setMatrix(id: TransformId, decomposed: NodeTransformView.Decomposed) {
        val currentTransform = transforms[id]
        val targetTransform: NodeTransform.Decomposed

        if (currentTransform is NodeTransform.Decomposed) {
            targetTransform = currentTransform
        } else {
            targetTransform = NodeTransform.Decomposed()
            transforms[id] = targetTransform // 替换旧的变换
            intermediateMatrices.getOrPut(id, ::Matrix4f)
        }

        targetTransform.set(decomposed)
        markDirty(id) // 标记为脏
    }
}
