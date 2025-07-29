package top.fifthlight.touchcontroller.common.ui.tab

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.Text
import top.fifthlight.touchcontroller.common.config.GlobalConfig
import top.fifthlight.touchcontroller.common.ui.model.ConfigScreenModel
import top.fifthlight.touchcontroller.common.ui.tab.general.ControlTab
import top.fifthlight.touchcontroller.common.ui.tab.general.DebugTab
import top.fifthlight.touchcontroller.common.ui.tab.general.RegularTab
import top.fifthlight.touchcontroller.common.ui.tab.general.TouchRingTab
import top.fifthlight.touchcontroller.common.ui.tab.layout.CustomControlLayoutTab
import top.fifthlight.touchcontroller.common.ui.tab.layout.ManageControlPresetsTab

typealias OnResetHandler = GlobalConfig.() -> GlobalConfig

data class TabOptions(
    private val titleId: Identifier,
    val group: TabGroup? = null,
    val index: Int,
    val openAsScreen: Boolean = false,
    val onReset: OnResetHandler? = null,
) {
    val title: Text
        @Composable
        get() = Text.translatable(titleId)
}

abstract class Tab : Screen {
    abstract val options: TabOptions

    companion object {
        fun getAllTabs(configScreenModel: ConfigScreenModel): PersistentList<Tab> {
            val itemTabs = ItemTabs(configScreenModel)
            return persistentListOf<Tab>(
                AboutTab,
                ManageControlPresetsTab,
                CustomControlLayoutTab,
                RegularTab,
                ControlTab,
                TouchRingTab,
                DebugTab,
                itemTabs.usableItemsTab,
                itemTabs.showCrosshairItemsTab,
                itemTabs.crosshairAimingItemsTab,
            )
        }
    }
}

