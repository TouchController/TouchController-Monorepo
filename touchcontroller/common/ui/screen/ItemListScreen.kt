package top.fifthlight.touchcontroller.common.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import kotlinx.collections.immutable.PersistentList
import org.koin.core.parameter.parametersOf
import top.fifthlight.combine.data.Item
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.placement.*
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.widget.base.layout.Box
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.ui.Icon
import top.fifthlight.combine.widget.ui.IconButton
import top.fifthlight.combine.widget.ui.Item
import top.fifthlight.combine.widget.ui.Text
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.common.ui.component.*
import top.fifthlight.touchcontroller.common.ui.model.ItemListScreenModel

class ItemListScreen(
    private val initialValue: PersistentList<Item>,
    private val onValueChanged: (PersistentList<Item>) -> Unit,
) : Screen {
    @Composable
    override fun Content() {
        val screenModel: ItemListScreenModel = koinScreenModel { parametersOf(initialValue, onValueChanged) }
        Scaffold(
            topBar = {
                AppBar(
                    modifier = Modifier.fillMaxWidth(),
                    leading = {
                        BackButton(
                            screenName = Text.translatable(Texts.SCREEN_ITEM_LIST_TITLE),
                        )
                    },
                )
            },
        ) { modifier ->
            Row(modifier) {
                Column(
                    modifier = Modifier
                        .padding(2)
                        .verticalScroll()
                        .border(Textures.WIDGET_BACKGROUND_BACKGROUND_DARK)
                        .fillMaxHeight()
                        .weight(.4f),
                ) {
                    val items by screenModel.value.collectAsState()
                    for ((index, item) in items.withIndex()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier
                                    .border(LocalListButtonDrawable.current.unchecked.normal)
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4),
                            ) {
                                Item(item = item)
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = item.toStack().name,
                                )
                            }

                            IconButton(
                                modifier = Modifier.fillMaxHeight(),
                                onClick = { screenModel.removeItem(index) },
                            ) {
                                Icon(Textures.ICON_DELETE)
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .border(Textures.WIDGET_BACKGROUND_BACKGROUND_DARK)
                        .fillMaxHeight()
                        .weight(.6f)
                ) {
                    ItemChooser(screenModel::addItem)
                }
            }
        }
    }
}