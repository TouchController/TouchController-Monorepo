package top.fifthlight.blazerod.render.gl

import com.mojang.blaze3d.opengl.GlStateManager
import net.minecraft.client.gl.CompiledShader
import net.minecraft.client.gl.ShaderLoader
import net.minecraft.client.gl.ShaderProgram
import org.lwjgl.opengl.GL20C
import org.slf4j.LoggerFactory

object ShaderProgramExt {
    private val LOGGER = LoggerFactory.getLogger(ShaderProgramExt::class.java)

    @JvmStatic
    @Throws(ShaderLoader.LoadException::class)
    fun create(computeShader: CompiledShader, name: String): ShaderProgram {
        val programId = GlStateManager.glCreateProgram().also { programId ->
            if (programId <= 0) {
                throw ShaderLoader.LoadException("Could not create shader program (returned program ID $programId)")
            }
        }
        GlStateManager.glAttachShader(programId, computeShader.handle)
        GlStateManager.glLinkProgram(programId)
        val linkStatus = GlStateManager.glGetProgrami(programId, GL20C.GL_LINK_STATUS)
        val infoLog = GlStateManager.glGetProgramInfoLog(programId, 32768)
        return if (linkStatus != 0 && "Failed for unknown reason" !in infoLog) {
            if (infoLog.isNotEmpty()) {
                LOGGER.info("Info log when linking program containing CS {}. Log output: {}", computeShader.id, infoLog)
            }

            ShaderProgram(programId, name)
        } else {
            throw ShaderLoader.LoadException("Error encountered when linking program containing CS ${computeShader.id}. Log output: $infoLog")
        }
    }
}