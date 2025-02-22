package top.fifthlight.touchcontroller.ui.tab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.drawing.border
import top.fifthlight.combine.modifier.placement.fillMaxHeight
import top.fifthlight.combine.modifier.placement.fillMaxWidth
import top.fifthlight.combine.modifier.placement.height
import top.fifthlight.combine.modifier.placement.padding
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.paint.Colors
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.ui.Icon
import top.fifthlight.combine.widget.ui.Link
import top.fifthlight.combine.widget.ui.Text
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.BuildInfo
import top.fifthlight.touchcontroller.about.License
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.ui.component.AppBar
import top.fifthlight.touchcontroller.ui.component.BackButton
import top.fifthlight.touchcontroller.ui.component.Scaffold
import top.fifthlight.touchcontroller.ui.component.SideTabBar
import top.fifthlight.touchcontroller.ui.model.AboutScreenModel
import top.fifthlight.touchcontroller.ui.screen.LicenseScreen

object AboutTab : Tab() {
    override val options = TabOptions(
        titleId = Texts.SCREEN_CONFIG_ABOUT_TITLE,
        group = null,
        index = 0,
    )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = koinScreenModel<AboutScreenModel>()
        val aboutInfo by screenModel.aboutInfo.collectAsState()
        Scaffold(
            topBar = {
                AppBar(
                    modifier = Modifier.fillMaxWidth(),
                    leading = {
                        BackButton(
                            screenName = Text.translatable(Texts.SCREEN_CONFIG_TITLE),
                            close = true
                        )
                    },
                    title = {
                        Text(Text.translatable(Texts.SCREEN_CONFIG_ABOUT_TITLE))
                    },
                )
            },
            sideBar = {
                SideTabBar(
                    modifier = Modifier.fillMaxHeight(),
                    onTabSelected = {
                        navigator?.replace(it)
                    }
                )
            },
        ) { modifier ->
            Column(
                modifier = Modifier
                    .padding(4)
                    .verticalScroll()
                    .then(modifier),
                verticalArrangement = Arrangement.spacedBy(8),
            ) {
                val iconSize = 32
                Row(
                    modifier = Modifier.fillMaxWidth().height(iconSize),
                    horizontalArrangement = Arrangement.spacedBy(8),
                ) {
                    Icon(
                        texture = Textures.CONTROL_CLASSIC_DPAD_UP,
                        size = IntSize(iconSize),
                    )
                    Column(
                        modifier = Modifier.weight(1f).height(iconSize),
                        verticalArrangement = Arrangement.SpaceAround,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4)) {
                            Text(Text.literal(BuildInfo.MOD_NAME).bold())
                            Text(BuildInfo.MOD_VERSION)
                        }
                        Text(BuildInfo.MOD_DESCRIPTION)
                    }
                }

                Column {
                    Row {
                        Text(Text.translatable(Texts.SCREEN_CONFIG_ABOUT_AUTHORS_TITLE))
                        Text(BuildInfo.MOD_AUTHORS)
                    }
                    Row {
                        Text(Text.translatable(Texts.SCREEN_CONFIG_ABOUT_CONTRIBUTORS_TITLE))
                        Text(BuildInfo.MOD_CONTRIBUTORS)
                    }
                    Row {
                        Text(Text.translatable(Texts.SCREEN_CONFIG_ABOUT_LICENSE_TITLE))
                        aboutInfo?.modLicense?.let { modLicense ->
                            val license = License(
                                name = BuildInfo.MOD_LICENSE,
                                content = modLicense,
                            )
                            Link(
                                text = BuildInfo.MOD_LICENSE,
                                onClick = {
                                    navigator?.push(LicenseScreen(license))
                                },
                            )
                        } ?: run {
                            Text(text = BuildInfo.MOD_LICENSE)
                        }
                    }
                }

                aboutInfo?.let { aboutInfo ->
                    val libraries = aboutInfo.libraries
                    if (libraries == null) {
                        return@let
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8)) {
                        for (library in libraries.libraries) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(Textures.WIDGET_TAB_TAB),
                                verticalArrangement = Arrangement.spacedBy(4),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(library.name)
                                    library.artifactVersion?.let { version ->
                                        Text(version, color = Colors.ALTERNATE_WHITE)
                                    } ?: run {
                                        Text(
                                            text = Text.translatable(Texts.SCREEN_CONFIG_ABOUT_UNKNOWN_VERSION),
                                            color = Colors.ALTERNATE_WHITE
                                        )
                                    }
                                }
                                Text(library.uniqueId, color = Colors.ALTERNATE_WHITE)
                                Row(horizontalArrangement = Arrangement.spacedBy(4)) {
                                    for (developer in library.developers) {
                                        developer.name?.let { name ->
                                            Text(
                                                text = name,
                                                color = Colors.ALTERNATE_WHITE
                                            )
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4, Alignment.Right)) {
                                    for (license in library.licenses) {
                                        val license = aboutInfo.libraries.licenses[license]
                                        license?.content?.let { content ->
                                            Link(
                                                text = license.name,
                                                onClick = { navigator?.push(LicenseScreen(license)) },
                                            )
                                        } ?: license?.name?.let { name ->
                                            Text(name)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } ?: run {
                    Text(Text.translatable(Texts.SCREEN_CONFIG_ABOUT_LOADING))
                }
            }
        }
    }
}