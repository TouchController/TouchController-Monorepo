package top.fifthlight.touchcontroller.common.ui.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import top.fifthlight.touchcontroller.common.about.AboutInfo
import top.fifthlight.touchcontroller.common.about.AboutInfoProvider

class AboutScreenModel : TouchControllerScreenModel() {
    private val logger = LoggerFactory.getLogger(AboutScreenModel::class.java)
    private val aboutInfoProvider: AboutInfoProvider by inject()
    private val _aboutInfo = MutableStateFlow<AboutInfo?>(null)
    val aboutInfo = _aboutInfo.asStateFlow()

    init {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    aboutInfoProvider.aboutInfo
                } catch (ex: Exception) {
                    logger.warn("Failed to read about information", ex)
                    null
                }
            }?.let { aboutInfo ->
                _aboutInfo.value = aboutInfo
            }
        }
    }
}