package top.fifthlight.touchcontroller.proxy.client

enum class PlatformCapability(val id: String) {
    /**
     * 传递文本输入信息。声明具有该能力后，会汇报游戏内文本编辑状态的改变。
     *
     * 通常来说，只需要实现 TextStatus 或者 KeyboardShow 二者其一。
     */
    TEXT_STATUS("text_status"),

    /**
     * 显示键盘。声明具有该能力后，会汇报是否需要显示软键盘。
     *
     * 通常来说，只需要实现 TextStatus 或者 KeyboardShow 二者其一。
     */
    KEYBOARD_SHOW("keyboard_show")
}
