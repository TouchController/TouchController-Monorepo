package top.fifthlight.touchcontroller.common_1_21_1_21_1

import net.minecraft.client.Minecraft
import org.koin.dsl.module
import top.fifthlight.combine.data.DataComponentTypeFactory
import top.fifthlight.combine.data.ItemFactory
import top.fifthlight.combine.data.TextFactory
import top.fifthlight.combine.paint.Canvas
import top.fifthlight.combine.paint.TextMeasurer
import top.fifthlight.combine.platform_1_21_1_21_1.CanvasImpl
import top.fifthlight.combine.platform_1_21_1_21_1.DataComponentTypeFactoryImpl
import top.fifthlight.combine.platform_1_21_1_21_1.ItemFactoryImpl
import top.fifthlight.combine.platform_1_21_1_21_5.ScreenFactoryImpl
import top.fifthlight.combine.platform_1_21_1_21_5.SoundManagerImpl
import top.fifthlight.combine.platform_1_21_x.TextFactoryImpl
import top.fifthlight.combine.platform_1_21_x.TextMeasurerImpl
import top.fifthlight.combine.screen.ScreenFactory
import top.fifthlight.combine.sound.SoundManager
import top.fifthlight.touchcontroller.common.config.GameConfigEditor
import top.fifthlight.touchcontroller.common.gal.*
import top.fifthlight.touchcontroller.common_1_21_1_21_1.gal.CrosshairRendererImpl
import top.fifthlight.touchcontroller.common_1_21_1_21_1.gal.DefaultItemListProviderImpl
import top.fifthlight.touchcontroller.common_1_21_1_21_1.gal.PlayerHandleFactoryImpl
import top.fifthlight.touchcontroller.common_1_21_1_21_1.gal.VanillaItemListProviderImpl
import top.fifthlight.touchcontroller.common_1_21_1_21_5.gal.GameActionImpl
import top.fifthlight.touchcontroller.common_1_21_x.GameConfigEditorImpl
import top.fifthlight.touchcontroller.common_1_21_x.gal.*

val platformModule = module {
    val client = Minecraft.getInstance()
    single<SoundManager> { SoundManagerImpl(client.soundManager) }
    single<ItemFactory> { ItemFactoryImpl }
    single<TextFactory> { TextFactoryImpl }
    single<DataComponentTypeFactory> { DataComponentTypeFactoryImpl }
    single<ScreenFactory> { ScreenFactoryImpl }
    single<GameConfigEditor> { GameConfigEditorImpl }
    single<CrosshairRenderer> { CrosshairRendererImpl }
    single<PlayerHandleFactory> { PlayerHandleFactoryImpl }
    single<ViewActionProvider> { ViewActionProviderImpl }
    single<GameAction> { GameActionImpl }
    single<GameFeatures> { gameFeatures }
    single<GameStateProvider> { GameStateProviderImpl }
    single<WindowHandle> { WindowHandleImpl }
    single<DefaultItemListProvider> { DefaultItemListProviderImpl }
    single<GameDispatcher> { GameDispatcherImpl }
    single<TextMeasurer> { TextMeasurerImpl }
    single<VanillaItemListProvider> { VanillaItemListProviderImpl }
    single<ChatMessageProvider> { ChatMessageProviderImpl }
    factory<Canvas> { params -> CanvasImpl(params.get()) }
}
