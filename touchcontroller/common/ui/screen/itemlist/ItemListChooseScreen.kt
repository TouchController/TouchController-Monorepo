package top.fifthlight.touchcontroller.common.ui.screen.itemlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import org.koin.compose.koinInject
import top.fifthlight.combine.data.Item
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.placement.fillMaxSize
import top.fifthlight.combine.modifier.placement.fillMaxWidth
import top.fifthlight.combine.widget.base.layout.Box
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.ui.Button
import top.fifthlight.combine.widget.ui.Text
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.common.gal.PlayerHandleFactory
import top.fifthlight.touchcontroller.common.gal.VanillaItemListProvider

class ItemListChooseScreen(
    private val onItemSelected: (Item) -> Unit,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        Box(
            modifier = Modifier.fillMaxSize(),
            alignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(.6f),
                verticalArrangement = Arrangement.spacedBy(8),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(Text.translatable(Texts.SCREEN_ITEM_LIST_CONTENT))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        navigator?.push(DefaultItemListScreen(onItemSelected))
                    }
                ) {
                    Text(Text.translatable(Texts.SCREEN_ITEM_LIST_DEFAULT))
                }

                val playerHandleFactory: PlayerHandleFactory = koinInject()
                val player = remember(playerHandleFactory) { playerHandleFactory.getPlayerHandle() }
                if (player != null) {
                    val vanillaItemListProvider: VanillaItemListProvider = koinInject()
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val tabs = vanillaItemListProvider.getCreativeTabs(player)
                            val playerInventory = player.getInventory()
                            navigator?.push(VanillaItemListScreen(onItemSelected, tabs, playerInventory))
                        }
                    ) {
                        Text(Text.translatable(Texts.SCREEN_ITEM_LIST_VANILLA_INVENTORY))
                    }
                }
            }
        }
    }
}