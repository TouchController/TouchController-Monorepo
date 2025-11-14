package top.fifthlight.touchcontroller.common.gal

import kotlinx.collections.immutable.PersistentList
import top.fifthlight.combine.data.ItemStack
import top.fifthlight.combine.data.Text

interface VanillaItemListProvider {
    interface CreativeTab {
        enum class Type {
            CATEGORY,
            SEARCH,
            SURVIVAL_INVENTORY,
        }

        val type: Type
        val name: Text
        val icon: ItemStack
        val items: PersistentList<ItemStack>
    }

    fun getCreativeTabs(player: PlayerHandle): PersistentList<CreativeTab>
}
