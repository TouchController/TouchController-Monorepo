package top.fifthlight.touchcontroller.common.ui.screen.itemlist

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.collections.immutable.toPersistentList
import top.fifthlight.combine.data.Item
import top.fifthlight.combine.data.LocalItemFactory
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.placement.fillMaxWidth
import top.fifthlight.combine.modifier.placement.padding
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.ui.EditText
import top.fifthlight.combine.widget.ui.ItemGrid
import top.fifthlight.touchcontroller.assets.Texts

class DefaultItemListScreen(
    val onItemSelected: (Item) -> Unit,
) : Screen {
    @Composable
    override fun Content() {
        val itemFactory = LocalItemFactory.current
        var searchText by remember { mutableStateOf("") }
        val allItems = remember(itemFactory) {
            itemFactory.allItems.map {
                Pair(it, itemFactory.createItemStack(it, 1))
            }.toPersistentList()
        }
        val showingItems = remember(searchText, allItems) {
            if (searchText.isEmpty()) {
                allItems
            } else {
                allItems.filter { (_, stack) ->
                    stack.name.string.contains(searchText, ignoreCase = true)
                }.toPersistentList()
            }
        }
        Column(
            modifier = Modifier.padding(4),
            verticalArrangement = Arrangement.spacedBy(8),
        ) {
            EditText(
                modifier = Modifier.fillMaxWidth(),
                placeholder = Text.translatable(Texts.SCREEN_ITEM_LIST_SEARCH_PLACEHOLDER),
                value = searchText,
                onValueChanged = { searchText = it }
            )

            ItemGrid(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                stacks = showingItems,
                onStackClicked = { item, stack -> onItemSelected(item) },
            )
        }
    }
}