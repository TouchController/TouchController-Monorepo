package top.fifthlight.touchcontroller.ui.component

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import kotlinx.collections.immutable.persistentListOf
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
import top.fifthlight.touchcontroller.ui.tab.Tab
import top.fifthlight.touchcontroller.ui.tab.TabGroup
import top.fifthlight.touchcontroller.ui.tab.TabOptions
import top.fifthlight.touchcontroller.ui.tab.allTabs

private val tabGroups by lazy {
    (persistentListOf(null) + TabGroup.allGroups).map { group ->
        Pair(group, allTabs.filter { it.options.group == group }.sortedBy { it.options.index })
    }
}

@Composable
fun SideTabBar(
    modifier: Modifier = Modifier,
    onTabSelected: (Tab, TabOptions) -> Unit,
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
                            }
                        ) {
                            Text(options.title)
                        }
                    }
                }
            }
        }
    }
}
