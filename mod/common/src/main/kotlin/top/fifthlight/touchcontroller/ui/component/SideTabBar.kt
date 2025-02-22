package top.fifthlight.touchcontroller.ui.component

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.placement.fillMaxWidth
import top.fifthlight.combine.modifier.placement.width
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.ui.Text
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.ui.tab.AboutTab
import top.fifthlight.touchcontroller.ui.tab.Tab
import top.fifthlight.touchcontroller.ui.tab.TabGroup

private val allTabs = persistentListOf<Tab>(
    AboutTab,
)

private val tabGroups by lazy {
    buildList {
        add(null)
        TabGroup.allTabs.forEach(::add)
    }.toPersistentList().map { group ->
        Pair(group, allTabs.filter { it.options.group == group }.sortedBy { it.options.index })
    }
}

@Composable
fun SideTabBar(
    modifier: Modifier = Modifier,
    onTabSelected: (Tab) -> Unit,
) {
    val navigator = LocalNavigator.current
    Column(
        modifier = Modifier
            .border(Textures.WIDGET_BACKGROUND_BACKGROUND_DARK)
            .width(130)
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(4),
    ) {
        for ((group, tabs) in tabGroups) {
            group?.let { group ->
                Text(group.title)
            }
            Column {
                for (tab in tabs) {
                    TabButton(
                        modifier = Modifier.fillMaxWidth(),
                        selected = navigator?.lastItem == tab,
                        onClick = { onTabSelected(tab) }
                    ) {
                        Text(tab.options.title)
                    }
                }
            }
        }
    }
}
