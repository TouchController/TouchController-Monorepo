package top.fifthlight.touchcontroller.common_1_21_3_1_21_4.event.gal

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import top.fifthlight.combine.platform_1_21_3_1_21_4.toCombine
import top.fifthlight.combine.platform_1_21_x.TextImpl
import top.fifthlight.touchcontroller.common.gal.PlayerHandle
import top.fifthlight.touchcontroller.common.gal.VanillaItemListProvider
import kotlin.jvm.optionals.getOrNull

object VanillaItemListProviderImpl : VanillaItemListProvider {
    private val client = Minecraft.getInstance()

    class CreativeTabImpl(group: CreativeModeTab) : VanillaItemListProvider.CreativeTab {
        override val type = when (group.type) {
            CreativeModeTab.Type.CATEGORY -> VanillaItemListProvider.CreativeTab.Type.CATEGORY
            CreativeModeTab.Type.INVENTORY -> VanillaItemListProvider.CreativeTab.Type.SURVIVAL_INVENTORY
            CreativeModeTab.Type.HOTBAR -> error("Bad item group")
            CreativeModeTab.Type.SEARCH -> VanillaItemListProvider.CreativeTab.Type.SEARCH
        }

        override val name = TextImpl(group.displayName)

        override val icon = group.iconItem.toCombine()

        override val items = group.displayItems.map { it.toCombine() }.toPersistentList()
    }

    val LocalPlayer.shouldShowOperatorTab
        get() = canUseGameMasterBlocks() && client.options.operatorItemsTab().get()

    fun LocalPlayer.refreshCreativeTabs(shouldShowOperatorTab: Boolean) {
        val enabledFeatures = connection.enabledFeatures()
        CreativeModeTabs.tryRebuildTabContents(enabledFeatures, shouldShowOperatorTab, level().registryAccess())
    }

    private val opBlocks = ResourceLocation.withDefaultNamespace("op_blocks")

    override fun getCreativeTabs(player: PlayerHandle): PersistentList<VanillaItemListProvider.CreativeTab> {
        val player = (player as PlayerHandleImpl).inner
        val shouldShowOperatorTab = player.shouldShowOperatorTab
        player.refreshCreativeTabs(shouldShowOperatorTab)
        return CreativeModeTabs
            .tabs()
            .filter {
                if (it.type == CreativeModeTab.Type.HOTBAR) {
                    return@filter false
                }
                val key = BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(it).getOrNull()
                if (key?.location() == opBlocks) {
                    return@filter shouldShowOperatorTab
                }
                true
            }
            .map(::CreativeTabImpl)
            .toPersistentList()
    }
}