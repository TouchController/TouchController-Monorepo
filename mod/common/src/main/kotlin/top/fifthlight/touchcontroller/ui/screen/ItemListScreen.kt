package top.fifthlight.touchcontroller.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import kotlinx.collections.immutable.PersistentList
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import top.fifthlight.combine.data.Item
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.placement.fillMaxHeight
import top.fifthlight.combine.modifier.placement.fillMaxSize
import top.fifthlight.combine.modifier.placement.fillMaxWidth
import top.fifthlight.combine.modifier.placement.padding
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.widget.base.layout.Box
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.ui.Item
import top.fifthlight.combine.widget.ui.Text
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.gal.PlayerHandleFactory
import top.fifthlight.touchcontroller.ui.component.*
import top.fifthlight.touchcontroller.ui.model.ItemListScreenModel
import top.fifthlight.touchcontroller.ui.screen.itemlist.DefaultItemListScreen
import top.fifthlight.touchcontroller.ui.screen.itemlist.ItemListChooseScreen

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
                            close = false
                        )
                    },
                )
            },
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
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
                        ListButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { screenModel.removeItem(index) },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Item(item = item)
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = item.toStack().name,
                                )
                                Text(Text.translatable(Texts.SCREEN_ITEM_LIST_REMOVE))
                            }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxHeight().weight(.6f)) {
                    val playerHandleFactory: PlayerHandleFactory = koinInject()
                    val playerHandle = playerHandleFactory.getPlayerHandle()
                    TouchControllerNavigator(
                        if (playerHandle == null) {
                            DefaultItemListScreen(screenModel::addItem)
                        } else {
                            ItemListChooseScreen(screenModel::addItem)
                        }
                    )
                }
            }
        }
    }
}