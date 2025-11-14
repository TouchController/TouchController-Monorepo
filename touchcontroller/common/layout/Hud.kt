package top.fifthlight.touchcontroller.common.layout

import top.fifthlight.touchcontroller.common.config.LayoutLayer

fun Context.Hud(layers: List<LayoutLayer>) {
    for (layer in layers) {
        if (!layer.conditions.check(input)) {
            continue
        }
        for (widget in layer.widgets) {
            withOpacity(widget.opacity) {
                withAlign(
                    align = widget.align,
                    offset = widget.offset,
                    size = widget.size()
                ) {
                    widget.layout(this)
                }
            }
        }
    }

    if (!input.inGui) {
        Inventory()
        View()
        Crosshair()
        if (config.debug.showPointers) {
            Pointers()
        }
    }
}