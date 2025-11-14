package top.fifthlight.touchcontroller.common.ui.tab.layout.custom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.collections.immutable.persistentListOf
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.placement.*
import top.fifthlight.combine.widget.base.layout.*
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.common.ui.component.TitleBox
import top.fifthlight.touchcontroller.common.ui.model.CustomControlLayoutTabModel
import top.fifthlight.touchcontroller.common.ui.state.CustomControlLayoutTabState

data class CustomTabContext(
    val screenModel: CustomControlLayoutTabModel,
    val uiState: CustomControlLayoutTabState.Enabled,
    val tabsButton: @Composable () -> Unit,
    val sideBarAtRight: Boolean,
    val parentNavigator: Navigator?,
)

val LocalCustomTabContext = compositionLocalOf<CustomTabContext> { error("No CustomTabContext") }

abstract class CustomTab : Screen {
    @Composable
    abstract fun Icon()

    @Composable
    fun SideBar(
        tabsButton: @Composable () -> Unit,
        actions: @Composable ColumnScope.() -> Unit
    ) {
        Column {
            tabsButton()
            Spacer(
                modifier = Modifier
                    .border(Textures.WIDGET_BACKGROUND_BACKGROUND_LIGHTGRAY_TITLE)
                    .weight(1f)
                    .width(22),
            )
            actions()
        }
    }

    @Composable
    fun SideBarContainer(
        sideBarAtRight: Boolean,
        tabsButton: @Composable () -> Unit,
        actions: @Composable ColumnScope.() -> Unit,
        content: @Composable RowScope.(Modifier) -> Unit,
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (sideBarAtRight) {
                SideBar(tabsButton, actions)
            }
            content(Modifier.weight(1f).fillMaxHeight())
            if (!sideBarAtRight) {
                SideBar(tabsButton, actions)
            }
        }
    }

    @Composable
    fun SideBarScaffold(
        modifier: Modifier = Modifier,
        title: @Composable () -> Unit,
        actions: @Composable (RowScope.() -> Unit)?,
        content: @Composable BoxScope.() -> Unit,
    ) {
        Column(modifier) {
            TitleBox(
                modifier = Modifier.fillMaxWidth()
            ) {
                title()
            }
            Box(
                modifier = Modifier
                    .border(Textures.WIDGET_BACKGROUND_BACKGROUND_DARK_TITLE)
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                content()
            }
            actions?.let { actions ->
                Row(
                    modifier = Modifier
                        .padding(2)
                        .border(Textures.WIDGET_BACKGROUND_BACKGROUND_DARK_TITLE)
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(2),
                ) {
                    actions()
                }
            }
        }
    }
}

val allCustomTabs = persistentListOf<CustomTab>(
    PropertiesTab,
    WidgetsTab,
    LayersTab,
    PresetsTab,
)
