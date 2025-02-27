package top.fifthlight.touchcontroller.ui.tab

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.persistentListOf
import top.fifthlight.combine.data.Identifier
import top.fifthlight.combine.data.Text
import top.fifthlight.touchcontroller.assets.Texts

sealed class TabGroup(
    val titleId: Identifier
) {
    val title: Text
        @Composable
        get() = Text.translatable(titleId)

    data object LayoutGroup : TabGroup(Texts.SCREEN_CONFIG_LAYOUT_TITLE)
    data object GeneralGroup : TabGroup(Texts.SCREEN_CONFIG_GENERAL_TITLE)
    data object ItemGroup : TabGroup(Texts.SCREEN_CONFIG_ITEM_TITLE)

    companion object {
        val allGroups by lazy {
            persistentListOf<TabGroup>(
                LayoutGroup,
                GeneralGroup,
                ItemGroup,
            )
        }
    }
}
