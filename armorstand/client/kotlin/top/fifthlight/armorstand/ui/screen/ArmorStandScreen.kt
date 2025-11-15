package top.fifthlight.armorstand.ui.screen

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import top.fifthlight.armorstand.ui.model.ViewModel
import top.fifthlight.armorstand.util.ThreadExecutorDispatcher

abstract class ArmorStandScreen<T: ArmorStandScreen<T, M>, M: ViewModel>(
    parent: Screen? = null,
    viewModelFactory: (CoroutineScope) -> M,
    title: Text,
): BaseArmorStandScreen<T>(parent, title) {
    val scope = CoroutineScope(ThreadExecutorDispatcher(MinecraftClient.getInstance()) + SupervisorJob())
    val viewModel = viewModelFactory(scope)

    override fun close() {
        super.close()
        scope.cancel()
    }
}