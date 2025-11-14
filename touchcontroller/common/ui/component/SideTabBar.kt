package top.fifthlight.touchcontroller.common.ui.component

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.placement.fillMaxWidth
import top.fifthlight.combine.modifier.placement.padding
import top.fifthlight.combine.modifier.placement.width
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.ui.Text
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.common.ui.tab.Tab
import top.fifthlight.touchcontroller.common.ui.tab.TabGroup
import top.fifthlight.touchcontroller.common.ui.tab.TabOptions

@Composable
fun SideTabBar(
    modifier: Modifier = Modifier,
    onTabSelected: (Tab, TabOptions) -> Unit,
    tabGroups: List<Pair<TabGroup?, List<Tab>>>,
) {
    val navigator = LocalNavigator.current
    Column(
        modifier = Modifier
            .width(130)
            .padding(2)
            .verticalScroll()
            .border(Textures.WIDGET_BACKGROUND_BACKGROUND_DARK)
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(12),
    ) {
        for ((group, tabs) in tabGroups) {
            Column(verticalArrangement = Arrangement.spacedBy(4)) {
                group?.let { group ->
                    Text(group.title)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4)) {
                    for (tab in tabs) {
                        val options = tab.options
                        TabButton(
                            modifier = Modifier.fillMaxWidth(),
                            checked = navigator?.lastItem == tab,
                            onClick = {
                                onTabSelected(tab, options)
                            },
                        ) {
                            Text(options.title)
                        }
                    }
                }
            }
        }
    }
}
