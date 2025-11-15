package top.fifthlight.blazerod.runtime.load

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.future
import top.fifthlight.blazerod.api.loader.ModelLoader
import top.fifthlight.blazerod.model.Model
import top.fifthlight.blazerod.runtime.RenderSceneImpl
import top.fifthlight.blazerod.util.dispatchers.BlazeRod
import top.fifthlight.mergetools.api.ActualConstructor
import top.fifthlight.mergetools.api.ActualImpl

@ActualImpl(ModelLoader::class)
object ModelLoaderImpl : ModelLoader {
    @JvmStatic
    @ActualConstructor("create")
    fun create() = this

    override suspend fun loadModel(model: Model): RenderSceneImpl? = coroutineScope {
        val loadInfo = ModelPreprocessor.preprocess(
            scope = this,
            loadDispatcher = Dispatchers.Default,
            model = model,
        ) ?: return@coroutineScope null
        val gpuInfo = ModelResourceLoader.load(
            scope = this,
            gpuDispatcher = Dispatchers.BlazeRod.Main,
            info = loadInfo,
        )
        SceneReconstructor.reconstruct(info = gpuInfo)
    }

    override fun loadModelAsFuture(model: Model) = CoroutineScope(Dispatchers.Default).future {
        loadModel(model)
    }
}