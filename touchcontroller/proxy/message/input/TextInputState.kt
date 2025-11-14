package top.fifthlight.touchcontroller.proxy.message.input

data class TextInputState(
    val text: String = "",
    val composition: TextRange = TextRange.EMPTY,
    val selection: TextRange = TextRange(text.length),
    val selectionLeft: Boolean = true,
) {
    init {
        require(composition.end <= text.length) { "composition region end ${composition.end} should not exceed text length ${text.length}" }
        require(selection.end <= text.length) { "selection region end ${selection.end} should not exceed text length ${text.length}" }
    }
}

val TextInputState.compositionText
    get() = text.substring(composition)
val TextInputState.selectionText
    get() = text.substring(selection)

fun TextInputState.doBackspace(): TextInputState = if (composition.length != 0) {
    // IME should handle this, not by us
    this
} else if (selection.length != 0) {
    copy(
        text = text.removeRange(selection),
        selection = TextRange(selection.start),
        composition = TextRange.EMPTY,
    )
} else if (selection.start > 0) {
    copy(
        text = text.removeRange(selection.start - 1, selection.start),
        selection = TextRange(selection.start - 1),
        composition = TextRange.EMPTY,
    )
} else {
    this
}

fun TextInputState.doDelete(): TextInputState = if (composition.length != 0) {
    // IME should handle this, not by us
    this
} else if (selection.length != 0) {
    copy(
        text = text.removeRange(selection),
        selection = TextRange(selection.start),
        composition = TextRange.EMPTY,
    )
} else if (selection.end < text.length) {
    copy(
        text = text.removeRange(selection.start, selection.start + 1),
        selection = TextRange(selection.start),
        composition = TextRange.EMPTY,
    )
} else {
    this
}

fun TextInputState.doHome(): TextInputState = if (composition.length != 0) {
    // IME should handle this, not by us
    this
} else {
    copy(
        selection = TextRange(0),
        composition = TextRange.EMPTY,
    )
}

fun TextInputState.doEnd(): TextInputState = if (composition.length != 0) {
    // IME should handle this, not by us
    this
} else {
    copy(
        selection = TextRange(text.length),
        composition = TextRange.EMPTY,
    )
}

fun TextInputState.doArrowLeft(): TextInputState = if (composition.length != 0) {
    // IME should handle this, not by us
    this
} else if (selection.length > 0) {
    copy(
        selection = TextRange(selection.start),
        composition = TextRange.EMPTY,
    )
} else if (selection.start > 0) {
    copy(
        selection = TextRange(selection.start - 1),
        composition = TextRange.EMPTY,
    )
} else {
    this
}

fun TextInputState.doArrowRight(): TextInputState = if (composition.length != 0) {
    // IME should handle this, not by us
    this
} else if (selection.length > 0) {
    copy(
        selection = TextRange(selection.end),
        composition = TextRange.EMPTY,
    )
} else if (selection.end < text.length) {
    copy(
        selection = TextRange(selection.end + 1),
        composition = TextRange.EMPTY,
    )
} else {
    this
}

fun TextInputState.doShiftLeft(): TextInputState = if (composition.length != 0) {
    // IME should handle this, not by us
    this
} else if (selection.length == 0) {
    if (selection.start > 0) {
        copy(
            selection = TextRange(selection.start - 1, 1),
            composition = TextRange.EMPTY,
            selectionLeft = true,
        )
    } else {
        this
    }
} else if (selectionLeft) {
    if (selection.start > 0) {
        copy(
            selection = TextRange(selection.start - 1, selection.length + 1),
            composition = TextRange.EMPTY,
            selectionLeft = true,
        )
    } else {
        this
    }
} else {
    if (selection.end > 0) {
        copy(
            selection = TextRange(selection.start, selection.length - 1),
            composition = TextRange.EMPTY,
            selectionLeft = false,
        )
    } else {
        this
    }
}

fun TextInputState.doShiftRight(): TextInputState = if (composition.length != 0) {
    // IME should handle this, not by us
    this
} else if (selection.length == 0) {
    if (selection.end < text.length) {
        copy(
            selection = TextRange(selection.start, 1),
            composition = TextRange.EMPTY,
            selectionLeft = false,
        )
    } else {
        this
    }
} else if (selectionLeft) {
    if (selection.start < text.length) {
        copy(
            selection = TextRange(selection.start + 1, selection.length - 1),
            composition = TextRange.EMPTY,
            selectionLeft = true,
        )
    } else {
        this
    }
} else {
    if (selection.end < text.length) {
        copy(
            selection = TextRange(selection.start, selection.length + 1),
            composition = TextRange.EMPTY,
            selectionLeft = false,
        )
    } else {
        this
    }
}

fun TextInputState.doShiftHome(): TextInputState = if (composition.length != 0) {
    // IME should handle this, not by us
    this
} else if (selectionLeft) {
    copy(
        selection = TextRange(0, selection.end),
        composition = TextRange.EMPTY,
        selectionLeft = true,
    )
} else {
    copy(
        selection = TextRange(0, selection.start),
        composition = TextRange.EMPTY,
        selectionLeft = true,
    )
}

fun TextInputState.doShiftEnd(): TextInputState = if (composition.length != 0) {
    // IME should handle this, not by us
    this
} else if (selectionLeft) {
    copy(
        selection = TextRange(selection.end, text.length - selection.end),
        composition = TextRange.EMPTY,
        selectionLeft = false,
    )
} else {
    copy(
        selection = TextRange(selection.start, text.length - selection.start),
        composition = TextRange.EMPTY,
        selectionLeft = false,
    )
}

