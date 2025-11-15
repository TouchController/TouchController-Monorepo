package top.fifthlight.blazerod.api.loader

import top.fifthlight.blazerod.api.resource.RenderScene
import top.fifthlight.blazerod.model.Model
import top.fifthlight.mergetools.api.ExpectFactory
import java.util.concurrent.CompletableFuture

interface ModelLoader {
    suspend fun loadModel(model: Model): RenderScene?

    fun loadModelAsFuture(model: Model): CompletableFuture<out RenderScene?>

    @ExpectFactory
    interface Factory {
        fun create(): ModelLoader
    }
}