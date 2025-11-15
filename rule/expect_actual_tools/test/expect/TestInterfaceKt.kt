package top.fifthlight.mergetools.test

import top.fifthlight.mergetools.api.ExpectFactory

interface TestInterfaceKt {
    val age: Int
    val name: String

    @ExpectFactory
    interface Factory {
        fun of(age: Int, name: String): TestInterfaceKt
    }
}
