package top.fifthlight.blazerod.model.bedrock.animation

import org.joml.Vector3f
import org.joml.Vector3fc
import top.fifthlight.blazerod.model.animation.AnimationChannel
import top.fifthlight.blazerod.model.animation.SingleFrameAnimationChannel
import top.fifthlight.blazerod.model.bedrock.molang.value.MolangValue
import top.fifthlight.blazerod.model.bedrock.molang.value.MolangVector3f

fun <T : Any, D> MolangAnimationChannel(
    type: AnimationChannel.Type<T, D>,
    typeData: D,
    valueMapper: (src: Vector3fc, dst: T) -> Unit,
    molangValue: MolangVector3f,
) = Vector3f().let { tempValue ->
    SingleFrameAnimationChannel(
        type = type,
        typeData = typeData,
        setValue = when (molangValue) {
            is MolangVector3f.Molang -> { context, state, result ->
                fun MolangValue.evaluate() = when (this) {
                    is MolangValue.Molang -> (state as BedrockAnimationState).evalExpressions(context, molang).toFloat()
                    is MolangValue.Plain -> value
                }
                tempValue.x = molangValue.x.evaluate()
                tempValue.y = molangValue.y.evaluate()
                tempValue.z = molangValue.z.evaluate()
                valueMapper(tempValue, result)
            }

            is MolangVector3f.Plain -> { _, _, result ->
                tempValue.set(molangValue.value)
                valueMapper(tempValue, result)
            }
        }
    )
}
