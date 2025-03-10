package top.fifthlight.touchcontroller.common_1_21_1_21_1.gal

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.Items
import top.fifthlight.combine.platform_1_21_1_21_1.DataComponentTypeImpl
import top.fifthlight.combine.platform_1_21_1_21_1.ItemFactoryImpl
import top.fifthlight.combine.platform_1_21_1_21_1.ItemImpl
import top.fifthlight.touchcontroller.common.config.ItemList
import top.fifthlight.touchcontroller.common.gal.DefaultItemListProvider

object DefaultItemListProviderImpl : DefaultItemListProvider {
    override val usableItems = ItemList(
        whitelist = persistentListOf(
            ItemImpl(Items.FISHING_ROD),
            ItemImpl(Items.SPYGLASS),
            ItemImpl(Items.MAP),
            ItemImpl(Items.SHIELD),
            ItemImpl(Items.KNOWLEDGE_BOOK),
            ItemImpl(Items.WRITABLE_BOOK),
            ItemImpl(Items.WRITTEN_BOOK),
            ItemImpl(Items.ENDER_EYE),
            ItemImpl(Items.ENDER_PEARL),
            ItemImpl(Items.POTION),
            ItemImpl(Items.MILK_BUCKET),
        ),
        blacklist = persistentListOf(
            ItemImpl(Items.ARROW),
            ItemImpl(Items.FIRE_CHARGE),
            ItemImpl(Items.SPECTRAL_ARROW),
            ItemImpl(Items.TIPPED_ARROW),
            ItemImpl(Items.FIREWORK_ROCKET),
        ),
        subclasses = persistentSetOf(
            ItemFactoryImpl.rangedWeaponSubclass,
            ItemFactoryImpl.projectileSubclass,
            ItemFactoryImpl.armorSubclass,
        ),
        components = persistentListOf(
            DataComponentTypeImpl(DataComponents.FOOD),
            DataComponentTypeImpl(DataComponents.BUNDLE_CONTENTS),
        )
    )

    override val showCrosshairItems = ItemList(
        whitelist = persistentListOf(
            ItemImpl(Items.ENDER_PEARL),
        ),
        blacklist = persistentListOf(
            ItemImpl(Items.FIREWORK_ROCKET),
            ItemImpl(Items.ARROW),
            ItemImpl(Items.FIRE_CHARGE),
            ItemImpl(Items.SPECTRAL_ARROW),
            ItemImpl(Items.TIPPED_ARROW),
        ),
        subclasses = persistentSetOf(
            ItemFactoryImpl.rangedWeaponSubclass,
            ItemFactoryImpl.projectileSubclass,
        )
    )

    override val crosshairAimingItems = ItemList(
        whitelist = persistentListOf(
            ItemImpl(Items.ENDER_EYE),
            ItemImpl(Items.GLASS_BOTTLE),
        ),
        subclasses = persistentSetOf(
            ItemFactoryImpl.bucketSubclass,
            ItemFactoryImpl.boatSubclass,
            ItemFactoryImpl.placeableOnWaterSubclass,
            ItemFactoryImpl.spawnEggSubclass,
        )
    )
}