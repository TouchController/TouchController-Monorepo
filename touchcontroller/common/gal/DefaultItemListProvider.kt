package top.fifthlight.touchcontroller.common.gal

import top.fifthlight.touchcontroller.common.config.ItemList

interface DefaultItemListProvider {
    val usableItems: ItemList
    val showCrosshairItems: ItemList
    val crosshairAimingItems: ItemList
}