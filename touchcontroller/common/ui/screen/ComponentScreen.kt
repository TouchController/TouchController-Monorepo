package top.fifthlight.touchcontroller.common.ui.screen

import androidx.compose.runtime.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import top.fifthlight.combine.data.DataComponentType
import top.fifthlight.combine.data.DataComponentTypeFactory
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.placement.*
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.ui.EditText
import top.fifthlight.combine.widget.ui.Icon
import top.fifthlight.combine.widget.ui.IconButton
import top.fifthlight.combine.widget.ui.Text
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.common.ui.component.*
import top.fifthlight.touchcontroller.common.ui.model.ComponentScreenModel

class ComponentScreen(
    private val initialValue: PersistentList<DataComponentType>,
    private val onValueChanged: (PersistentList<DataComponentType>) -> Unit,
): Screen {
    @Composable
    override fun Content() {
        val screenModel: ComponentScreenModel = koinScreenModel { parametersOf(initialValue, onValueChanged) }
        Scaffold(
            topBar = {
                AppBar(
                    modifier = Modifier.fillMaxWidth(),
                    leading = {
                        BackButton(
                            screenName = Text.translatable(Texts.SCREEN_COMPONENT_LIST_TITLE),
                        )
                    },
                )
            },
        ) { modifier ->
            Row(modifier) {
                val items by screenModel.value.collectAsState()
                Column(
                    modifier = Modifier
                        .padding(2)
                        .verticalScroll()
                        .border(Textures.WIDGET_BACKGROUND_BACKGROUND_DARK)
                        .fillMaxHeight()
                        .weight(1f),
                ) {
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
                                val items = remember(item) { item.allItems }
                                ItemShower(items = items)
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = item.id.toString()
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
                Column(
                    modifier = Modifier
                        .padding(4)
                        .border(Textures.WIDGET_BACKGROUND_BACKGROUND_DARK)
                        .fillMaxHeight()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4),
                ) {
                    var searchText by remember { mutableStateOf("") }

                    EditText(
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = Text.translatable(Texts.SCREEN_COMPONENT_LIST_SEARCH_PLACEHOLDER),
                        value = searchText,
                        onValueChanged = { searchText = it }
                    )

                    Column(
                        modifier = Modifier
                            .verticalScroll()
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        val dataComponentTypeFactory: DataComponentTypeFactory = koinInject()
                        val dataComponentTypes =
                            remember(dataComponentTypeFactory) { dataComponentTypeFactory.allComponents }
                        val showingTypes = remember(dataComponentTypes, searchText, items) {
                            dataComponentTypes.filter {
                                if (it in items) {
                                    return@filter false
                                }
                                if (searchText.isNotEmpty() && !it.id.toString()
                                        .contains(searchText, ignoreCase = true)
                                ) {
                                    return@filter false
                                }
                                true
                            }.toPersistentList()
                        }
                        for (item in showingTypes) {
                            ListButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { screenModel.addItem(item) },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val items = remember(item) { item.allItems }
                                    ItemShower(items = items)
                                    Text(
                                        modifier = Modifier.weight(1f),
                                        text = item.id.toString()
                                    )
                                    Text(Text.translatable(Texts.SCREEN_COMPONENT_LIST_ADD))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}