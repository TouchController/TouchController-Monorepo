package top.fifthlight.touchcontroller.common.ui.component

import androidx.compose.runtime.Composable
import org.koin.compose.koinInject
import top.fifthlight.combine.data.Item
import top.fifthlight.touchcontroller.common.gal.PlayerHandleFactory
import top.fifthlight.touchcontroller.common.ui.screen.itemlist.DefaultItemListScreen
import top.fifthlight.touchcontroller.common.ui.screen.itemlist.ItemListChooseScreen

@Composable
fun ItemChooser(onItemChosen: (Item) -> Unit) {
    val playerHandleFactory: PlayerHandleFactory = koinInject()
    val playerHandle = playerHandleFactory.getPlayerHandle()
    TouchControllerNavigator(
        if (playerHandle == null) {
            DefaultItemListScreen(onItemChosen)
        } else {
            ItemListChooseScreen(onItemChosen)
        }
    )
}