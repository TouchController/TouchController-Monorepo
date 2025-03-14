package top.fifthlight.touchcontroller.common.control

import androidx.compose.runtime.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.fifthlight.combine.data.LocalTextFactory
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.data.TextFactory
import top.fifthlight.combine.data.Texture
import top.fifthlight.combine.layout.Alignment
import top.fifthlight.combine.layout.Arrangement
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.modifier.placement.fillMaxWidth
import top.fifthlight.combine.modifier.placement.width
import top.fifthlight.combine.modifier.scroll.verticalScroll
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.paint.Colors
import top.fifthlight.combine.widget.base.layout.Column
import top.fifthlight.combine.widget.base.layout.FlowRow
import top.fifthlight.combine.widget.base.layout.Row
import top.fifthlight.combine.widget.base.layout.Spacer
import top.fifthlight.combine.widget.ui.*
import top.fifthlight.data.IntOffset
import top.fifthlight.data.IntPadding
import top.fifthlight.data.IntSize
import top.fifthlight.touchcontroller.assets.EmptyTexture
import top.fifthlight.touchcontroller.assets.Texts
import top.fifthlight.touchcontroller.assets.Textures
import top.fifthlight.touchcontroller.common.annoations.DontTranslate
import top.fifthlight.touchcontroller.common.layout.Align

@Immutable
class BooleanProperty<Config : ControllerWidget>(
    private val getValue: (Config) -> Boolean,
    private val setValue: (Config, Boolean) -> Config,
    private val name: Text
) : ControllerWidget.Property<Config, Boolean>, KoinComponent {
    @Composable
    override fun controller(modifier: Modifier, config: ControllerWidget, onConfigChanged: (ControllerWidget) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        val widgetConfig = config as Config
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name)
            Spacer(modifier.weight(1f))
            Switch(
                value = getValue(widgetConfig),
                onValueChanged = {
                    onConfigChanged(setValue(widgetConfig, it))
                }
            )
        }
    }
}

@Immutable
class AnchorProperty<Config : ControllerWidget> : ControllerWidget.Property<Config, Align>, KoinComponent {
    @Composable
    override fun controller(modifier: Modifier, config: ControllerWidget, onConfigChanged: (ControllerWidget) -> Unit) {
        @Composable
        fun getItemText(align: Align): Text = when(align) {
            Align.LEFT_TOP -> Text.translatable(Texts.WIDGET_GENERAL_PROPERTY_ANCHOR_TOP_LEFT)
            Align.LEFT_CENTER -> Text.translatable(Texts.WIDGET_GENERAL_PROPERTY_ANCHOR_CENTER_LEFT)
            Align.LEFT_BOTTOM -> Text.translatable(Texts.WIDGET_GENERAL_PROPERTY_ANCHOR_BOTTOM_LEFT)
            Align.CENTER_TOP -> Text.translatable(Texts.WIDGET_GENERAL_PROPERTY_ANCHOR_TOP_CENTER)
            Align.CENTER_CENTER -> Text.translatable(Texts.WIDGET_GENERAL_PROPERTY_ANCHOR_CENTER_CENTER)
            Align.CENTER_BOTTOM -> Text.translatable(Texts.WIDGET_GENERAL_PROPERTY_ANCHOR_BOTTOM_CENTER)
            Align.RIGHT_TOP -> Text.translatable(Texts.WIDGET_GENERAL_PROPERTY_ANCHOR_TOP_RIGHT)
            Align.RIGHT_CENTER -> Text.translatable(Texts.WIDGET_GENERAL_PROPERTY_ANCHOR_CENTER_RIGHT)
            Align.RIGHT_BOTTOM -> Text.translatable(Texts.WIDGET_GENERAL_PROPERTY_ANCHOR_BOTTOM_RIGHT)
        }

        @Composable
        fun getItemIcon(align: Align): Texture = when(align) {
            Align.LEFT_TOP -> Textures.ICON_UP_LEFT
            Align.LEFT_CENTER -> Textures.ICON_LEFT
            Align.LEFT_BOTTOM -> Textures.ICON_DOWN_LEFT
            Align.CENTER_TOP -> Textures.ICON_UP
            Align.CENTER_CENTER -> Textures.ICON_MIDDLE
            Align.CENTER_BOTTOM -> Textures.ICON_DOWN
            Align.RIGHT_TOP -> Textures.ICON_UP_RIGHT
            Align.RIGHT_CENTER -> Textures.ICON_RIGHT
            Align.RIGHT_BOTTOM -> Textures.ICON_DOWN_RIGHT
        }

        @Suppress("UNCHECKED_CAST")
        val widgetConfig = config as Config
        Column(modifier) {
            Text(Text.translatable(Texts.WIDGET_GENERAL_PROPERTY_ANCHOR_NAME))

            var expanded by remember { mutableStateOf(false) }
            Select(
                modifier = Modifier.fillMaxWidth(),
                expanded = expanded,
                onExpandedChanged = { expanded = it },
                dropDownContent = {
                    val buttonWidth = contentWidth / 3
                    @Composable
                    fun AnchorButton(
                        anchor: Align
                    ) = IconButton(
                        minSize = IntSize(
                            width = buttonWidth,
                            height = 20,
                        ),
                        selected = config.align == anchor,
                        onClick = {
                            onConfigChanged(config.cloneBase(
                                align = anchor,
                                offset = IntOffset.ZERO,
                            ))
                        }
                    ) {
                        Icon(getItemIcon(anchor))
                    }

                    Column(
                        modifier = Modifier.width(contentWidth),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4),
                    ) {
                        FlowRow(maxColumns = 3) {
                            for (align in Align.entries){
                                AnchorButton(align)
                            }
                        }
                    }
                }
            ) {
                Text(getItemText(widgetConfig.align))
                Spacer(modifier = Modifier.weight(1f))
                SelectIcon(expanded = expanded)
            }
        }
    }
}

@Immutable
class EnumProperty<Config : ControllerWidget, T>(
    private val getValue: (Config) -> T,
    private val setValue: (Config, T) -> Config,
    private val name: Text,
    private val items: List<Pair<T, Text>>,
) : ControllerWidget.Property<Config, T>, KoinComponent {
    private val textFactory: TextFactory by inject()

    private fun getItemText(item: T): Text =
        items.firstOrNull { it.first == item }?.second ?: @DontTranslate textFactory.literal(item.toString())

    @Composable
    override fun controller(modifier: Modifier, config: ControllerWidget, onConfigChanged: (ControllerWidget) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        val widgetConfig = config as Config
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(4),
        ) {
            Text(name)

            var expanded by remember { mutableStateOf(false) }
            Select(
                modifier = Modifier.fillMaxWidth(),
                expanded = expanded,
                onExpandedChanged = { expanded = it },
                dropDownContent = {
                    val value = getValue(widgetConfig)
                    val selectedIndex = items.indexOfFirst { it.first == value }
                    DropdownItemList(
                        modifier = Modifier.verticalScroll(),
                        items = items,
                        textProvider = Pair<T, Text>::second,
                        selectedIndex = selectedIndex,
                        onItemSelected = {
                            val item = items[it].first
                            onConfigChanged(setValue(widgetConfig, item))
                            expanded = false
                        }
                    )
                }
            ) {
                Text(getItemText(getValue(widgetConfig)))
                Spacer(modifier = Modifier.weight(1f))
                SelectIcon(expanded = expanded)
            }
        }
    }
}

@Immutable
class FloatProperty<Config : ControllerWidget>(
    private val getValue: (Config) -> Float,
    private val setValue: (Config, Float) -> Config,
    private val range: ClosedFloatingPointRange<Float> = 0f..1f,
    private val messageFormatter: (Float) -> Text,
) : ControllerWidget.Property<Config, Float> {

    @Composable
    override fun controller(modifier: Modifier, config: ControllerWidget, onConfigChanged: (ControllerWidget) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        val widgetConfig = config as Config
        Column(modifier) {
            val value = getValue(widgetConfig)
            Text(messageFormatter(value))
            Slider(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                range = range,
                onValueChanged = {
                    onConfigChanged(setValue(widgetConfig, it))
                }
            )
        }
    }
}

@Immutable
class IntProperty<Config : ControllerWidget>(
    private val getValue: (Config) -> Int,
    private val setValue: (Config, Int) -> Config,
    private val range: IntRange,
    private val messageFormatter: (Int) -> Text,
) : ControllerWidget.Property<Config, Int> {
    @Composable
    override fun controller(modifier: Modifier, config: ControllerWidget, onConfigChanged: (ControllerWidget) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        val widgetConfig = config as Config
        Column(modifier) {
            val value = getValue(widgetConfig)
            Text(messageFormatter(value))
            IntSlider(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                range = range,
                onValueChanged = {
                    onConfigChanged(setValue(widgetConfig, it))
                }
            )
        }
    }
}

@Immutable
class StringProperty<Config : ControllerWidget>(
    private val getValue: (Config) -> String,
    private val setValue: (Config, String) -> Config,
    private val name: Text,
) : ControllerWidget.Property<Config, IntPadding> {
    @Composable
    override fun controller(modifier: Modifier, config: ControllerWidget, onConfigChanged: (ControllerWidget) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        val widgetConfig = config as Config
        val value = getValue(widgetConfig)
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(4),
        ) {
            Text(name)
            EditText(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                onValueChanged = { onConfigChanged(setValue(config, it)) }
            )
        }
    }
}

@Immutable
class ColorProperty<Config : ControllerWidget>(
    private val getValue: (Config) -> Color,
    private val setValue: (Config, Color) -> Config,
    private val name: Text,
) : ControllerWidget.Property<Config, IntPadding> {
    @Composable
    override fun controller(modifier: Modifier, config: ControllerWidget, onConfigChanged: (ControllerWidget) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        val widgetConfig = config as Config
        val value = getValue(widgetConfig)
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = name,
            )
            ColorPicker(
                value = value,
                onValueChanged = { onConfigChanged(setValue(config, it)) }
            )
        }
    }
}

@Immutable
class IntPaddingProperty<Config : ControllerWidget>(
    private val getValue: (Config) -> IntPadding,
    private val setValue: (Config, IntPadding) -> Config,
    private val name: Text,
) : ControllerWidget.Property<Config, IntPadding> {
    @Composable
    override fun controller(modifier: Modifier, config: ControllerWidget, onConfigChanged: (ControllerWidget) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        val widgetConfig = config as Config
        val value = getValue(widgetConfig)
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(4),
        ) {
            @Composable
            fun PaddingItem(
                text: Text,
                getSize: (IntPadding) -> Int,
                setSize: (IntPadding, Int) -> IntPadding,
            ) {
                val sizeValue = getSize(value)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text)
                    IntSlider(
                        modifier = Modifier.weight(1f),
                        range = 0..32,
                        value = sizeValue,
                        onValueChanged = {
                            onConfigChanged(setValue(config, setSize(value, it)))
                        }
                    )
                    var text by remember(sizeValue) { mutableStateOf(sizeValue.toString()) }
                    LaunchedEffect(text) {
                        val newSize = text.toIntOrNull() ?: return@LaunchedEffect
                        if (newSize == sizeValue) {
                            return@LaunchedEffect
                        }
                        onConfigChanged(setValue(config, setSize(value, newSize)))
                    }
                    EditText(
                        modifier = Modifier.width(48),
                        value = text,
                        onValueChanged = { text = it },
                    )
                }
            }

            Text(name)

            PaddingItem(
                text = Text.translatable(Texts.WIDGET_TEXTURE_EXTRA_PADDING_LEFT),
                getSize = IntPadding::left,
                setSize = { padding, size -> padding.copy(left = size) },
            )
            PaddingItem(
                text = Text.translatable(Texts.WIDGET_TEXTURE_EXTRA_PADDING_TOP),
                getSize = IntPadding::top,
                setSize = { padding, size -> padding.copy(top = size) },
            )
            PaddingItem(
                text = Text.translatable(Texts.WIDGET_TEXTURE_EXTRA_PADDING_RIGHT),
                getSize = IntPadding::right,
                setSize = { padding, size -> padding.copy(right = size) },
            )
            PaddingItem(
                text = Text.translatable(Texts.WIDGET_TEXTURE_EXTRA_PADDING_BOTTOM),
                getSize = IntPadding::bottom,
                setSize = { padding, size -> padding.copy(bottom = size) },
            )
        }
    }
}

@Immutable
class ButtonTextureProperty<Config : ControllerWidget>(
    private val getValue: (Config) -> ButtonTexture,
    private val setValue: (Config, ButtonTexture) -> Config,
    private val name: Text,
) : ControllerWidget.Property<Config, ButtonTexture>, KoinComponent {
    private val textFactory: TextFactory by inject()

    private fun <Texture : ButtonTexture> paddingProperty(
        getPadding: (ButtonTexture) -> IntPadding?,
        setPadding: (ButtonTexture, IntPadding) -> Texture,
        name: Text,
    ) = IntPaddingProperty<Config>(
        getValue = { getPadding(getValue(it)) ?: IntPadding.ZERO },
        setValue = { config, value -> setValue(config, setPadding(getValue(config), value)) },
        name = name,
    )

    private fun <Texture : ButtonTexture> intProperty(
        getInt: (ButtonTexture) -> Int?,
        setInt: (ButtonTexture, Int) -> Texture,
        range: IntRange,
        name: Text,
    ) = IntProperty<Config>(
        getValue = { getInt(getValue(it)) ?: 0 },
        setValue = { config, value -> setValue(config, setInt(getValue(config), value)) },
        range = range,
        messageFormatter = { textFactory.format(Texts.SCREEN_CONFIG_VALUE, textFactory.toNative(name), it.toString()) },
    )

    private fun <Texture : ButtonTexture> colorProperty(
        getColor: (ButtonTexture) -> Color?,
        setColor: (ButtonTexture, Color) -> Texture,
        name: Text,
    ) = ColorProperty<Config>(
        getValue = { getColor(getValue(it)) ?: Colors.BLACK },
        setValue = { config, value -> setValue(config, setColor(getValue(config), value)) },
        name = name,
    )

    private fun <Texture : ButtonTexture> scaleProperty(
        getScale: (ButtonTexture) -> Float?,
        setScale: (ButtonTexture, Float) -> Texture,
        range: ClosedFloatingPointRange<Float> = 0f..1f,
        name: Text,
    ) = FloatProperty<Config>(
        getValue = { getScale(getValue(it)) ?: 0f },
        setValue = { config, value -> setValue(config, setScale(getValue(config), value)) },
        range = range,
        messageFormatter = {
            textFactory.format(
                Texts.SCREEN_CONFIG_PERCENT,
                textFactory.toNative(name),
                it.toString()
            )
        },
    )

    private fun <Texture : ButtonTexture, T> enumProperty(
        getEnum: (ButtonTexture) -> T?,
        setEnum: (ButtonTexture, T) -> Texture,
        defaultValue: T,
        name: Text,
        items: List<Pair<T, Text>>,
    ) = EnumProperty<Config, T>(
        getValue = { getEnum(getValue(it)) ?: defaultValue },
        setValue = { config, value -> setValue(config, setEnum(getValue(config), value)) },
        name = name,
        items = items,
    )

    private val emptyTexturePaddingProperty = paddingProperty(
        getPadding = { (it as? ButtonTexture.Empty)?.extraPadding },
        setPadding = { texture, value ->
            when (texture) {
                is ButtonTexture.Empty -> texture.copy(extraPadding = value)
                else -> ButtonTexture.Empty(extraPadding = value)
            }
        },
        name = textFactory.of(Texts.WIDGET_TEXTURE_EXTRA_PADDING),
    )

    private val fillTextureBorderWidthProperty = intProperty(
        getInt = { (it as? ButtonTexture.Fill)?.borderWidth },
        setInt = { texture, value ->
            when (texture) {
                is ButtonTexture.Fill -> texture.copy(borderWidth = value)
                else -> ButtonTexture.Fill(borderWidth = value)
            }
        },
        range = 0..16,
        name = textFactory.of(Texts.WIDGET_TEXTURE_FILL_BORDER_WIDTH),
    )

    private val fillTexturePaddingProperty = paddingProperty(
        getPadding = { (it as? ButtonTexture.Fill)?.extraPadding },
        setPadding = { texture, value ->
            when (texture) {
                is ButtonTexture.Fill -> texture.copy(extraPadding = value)
                else -> ButtonTexture.Fill(extraPadding = value)
            }
        },
        name = textFactory.of(Texts.WIDGET_TEXTURE_EXTRA_PADDING),
    )

    private val fillTextureBorderColorProperty = colorProperty(
        getColor = { (it as? ButtonTexture.Fill)?.borderColor },
        setColor = { texture, value ->
            when (texture) {
                is ButtonTexture.Fill -> texture.copy(borderColor = value)
                else -> ButtonTexture.Fill(borderColor = value)
            }
        },
        name = textFactory.of(Texts.WIDGET_TEXTURE_FILL_BORDER_COLOR),
    )

    private val fillTextureBackgroundColorProperty = colorProperty(
        getColor = { (it as? ButtonTexture.Fill)?.backgroundColor },
        setColor = { texture, value ->
            when (texture) {
                is ButtonTexture.Fill -> texture.copy(backgroundColor = value)
                else -> ButtonTexture.Fill(backgroundColor = value)
            }
        },
        name = textFactory.of(Texts.WIDGET_TEXTURE_FILL_BACKGROUND_COLOR),
    )

    private val fixedTextureScaleProperty = scaleProperty(
        getScale = { (it as? ButtonTexture.Fixed)?.scale },
        setScale = { texture, value ->
            when (texture) {
                is ButtonTexture.Fixed -> texture.copy(scale = value)
                else -> ButtonTexture.Fixed(scale = value)
            }
        },
        name = textFactory.of(Texts.WIDGET_TEXTURE_FIXED_SCALE),
    )

    private val ninePatchTextureTextureProperty = enumProperty(
        getEnum = { (it as? ButtonTexture.NinePatch)?.texture },
        setEnum = { texture, value ->
            when (texture) {
                is ButtonTexture.NinePatch -> texture.copy(texture = value)
                else -> ButtonTexture.NinePatch(texture = value)
            }
        },
        defaultValue = EmptyTexture.EMPTY_1,
        name = textFactory.of(Texts.WIDGET_TEXTURE_NINE_PATCH_TEXTURE),
        items = EmptyTexture.entries.map { Pair(it, textFactory.of(it.nameId)) },
    )

    private val ninePatchTexturePaddingProperty = paddingProperty(
        getPadding = { (it as? ButtonTexture.NinePatch)?.extraPadding },
        setPadding = { texture, value ->
            when (texture) {
                is ButtonTexture.NinePatch -> texture.copy(extraPadding = value)
                else -> ButtonTexture.NinePatch(extraPadding = value)
            }
        },
        name = textFactory.of(Texts.WIDGET_TEXTURE_EXTRA_PADDING),
    )

    @Composable
    override fun controller(modifier: Modifier, config: ControllerWidget, onConfigChanged: (ControllerWidget) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        val widgetConfig = config as Config
        val value = getValue(widgetConfig)
        val textFactory = LocalTextFactory.current
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(4),
        ) {
            Text(name)
            var expanded by remember { mutableStateOf(false) }
            Select(
                modifier = Modifier.fillMaxWidth(),
                expanded = expanded,
                onExpandedChanged = { expanded = it },
                dropDownContent = {
                    DropdownItemList(
                        modifier = Modifier.verticalScroll(),
                        items = ButtonTexture.Type.entries,
                        textProvider = { textFactory.of(it.nameId) },
                        selectedIndex = ButtonTexture.Type.entries.indexOf(value.type),
                        onItemSelected = {
                            val item = ButtonTexture.Type.entries[it]
                            if (value.type != item) {
                                onConfigChanged(
                                    setValue(
                                        widgetConfig, when (item) {
                                            ButtonTexture.Type.EMPTY -> ButtonTexture.Empty()
                                            ButtonTexture.Type.FILL -> ButtonTexture.Fill()
                                            ButtonTexture.Type.FIXED -> ButtonTexture.Fixed()
                                            ButtonTexture.Type.NINE_PATCH -> ButtonTexture.NinePatch()
                                        }
                                    )
                                )
                            }
                            expanded = false
                        }
                    )
                }
            ) {
                Text(Text.translatable(value.type.nameId))
                Spacer(modifier = Modifier.weight(1f))
                SelectIcon(expanded = expanded)
            }

            @Composable
            fun <Config : ControllerWidget> ControllerWidget.Property<Config, *>.controller() = controller(
                modifier = Modifier.fillMaxWidth(),
                config = config,
                onConfigChanged = onConfigChanged,
            )

            when (value) {
                is ButtonTexture.Empty -> {
                    emptyTexturePaddingProperty.controller()
                }

                is ButtonTexture.Fill -> {
                    fillTexturePaddingProperty.controller()
                    fillTextureBorderWidthProperty.controller()
                    fillTextureBorderColorProperty.controller()
                    fillTextureBackgroundColorProperty.controller()
                }

                is ButtonTexture.Fixed -> {
                    fixedTextureScaleProperty.controller()
                }

                is ButtonTexture.NinePatch -> {
                    ninePatchTextureTextureProperty.controller()
                    ninePatchTexturePaddingProperty.controller()
                }
            }
        }
    }
}

@Immutable
class ButtonActiveTextureProperty<Config : ControllerWidget>(
    private val getValue: (Config) -> ButtonActiveTexture,
    private val setValue: (Config, ButtonActiveTexture) -> Config,
    private val name: Text,
) : ControllerWidget.Property<Config, ButtonActiveTexture>, KoinComponent {
    private val textFactory: TextFactory by inject()

    private fun <Texture : ButtonActiveTexture> buttonTextureProperty(
        getTexture: (ButtonActiveTexture) -> ButtonTexture?,
        setTexture: (ButtonActiveTexture, ButtonTexture) -> Texture,
        name: Text,
    ) = ButtonTextureProperty<Config>(
        getValue = { getTexture(getValue(it)) ?: ButtonTexture.Empty() },
        setValue = { config, value -> setValue(config, setTexture(getValue(config), value)) },
        name = name,
    )

    private val textureProperty = buttonTextureProperty(
        getTexture = { (it as? ButtonActiveTexture.Texture)?.texture },
        setTexture = { texture, value ->
            when (texture) {
                is ButtonActiveTexture.Texture -> texture.copy(texture = value)
                else -> ButtonActiveTexture.Texture(texture = value)
            }
        },
        name = textFactory.of(Texts.WIDGET_ACTIVE_TEXTURE_TYPE),
    )

    @Composable
    override fun controller(modifier: Modifier, config: ControllerWidget, onConfigChanged: (ControllerWidget) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        val widgetConfig = config as Config
        val value = getValue(widgetConfig)
        val textFactory = LocalTextFactory.current
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(4),
        ) {
            Text(name)
            var expanded by remember { mutableStateOf(false) }
            Select(
                modifier = Modifier.fillMaxWidth(),
                expanded = expanded,
                onExpandedChanged = { expanded = it },
                dropDownContent = {
                    DropdownItemList(
                        modifier = Modifier.verticalScroll(),
                        items = ButtonActiveTexture.Type.entries,
                        textProvider = { textFactory.of(it.nameId) },
                        selectedIndex = ButtonActiveTexture.Type.entries.indexOf(value.type),
                        onItemSelected = {
                            val item = ButtonActiveTexture.Type.entries[it]
                            if (value.type != item) {
                                onConfigChanged(
                                    setValue(
                                        widgetConfig, when (item) {
                                            ButtonActiveTexture.Type.SAME -> ButtonActiveTexture.Same
                                            ButtonActiveTexture.Type.GRAY -> ButtonActiveTexture.Gray
                                            ButtonActiveTexture.Type.TEXTURE -> ButtonActiveTexture.Texture()
                                        }
                                    )
                                )
                            }
                            expanded = false
                        }
                    )
                }
            ) {
                Text(Text.translatable(value.type.nameId))
                Spacer(modifier = Modifier.weight(1f))
                SelectIcon(expanded = expanded)
            }

            @Composable
            fun <Config : ControllerWidget> ControllerWidget.Property<Config, *>.controller() = controller(
                modifier = Modifier.fillMaxWidth(),
                config = config,
                onConfigChanged = onConfigChanged,
            )

            if (value is ButtonActiveTexture.Texture) {
                textureProperty.controller()
            }
        }
    }
}