package top.fifthlight.touchcontroller.common.ui.model

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.plus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import top.fifthlight.combine.data.DataComponentType

class ComponentScreenModel(
    initialValue: PersistentList<DataComponentType>,
    private val onValueChanged: (PersistentList<DataComponentType>) -> Unit,
) : TouchControllerScreenModel() {
    private val _value = MutableStateFlow(initialValue)
    val value = _value.asStateFlow()

    fun addItem(item: DataComponentType) {
        val newValue = _value.updateAndGet {
            if (item !in it) {
                it + item
            } else {
                it
            }
        }
        onValueChanged(newValue)
    }

    fun removeItem(index: Int) {
        val newValue = _value.updateAndGet { it.removeAt(index) }
        onValueChanged(newValue)
    }
}