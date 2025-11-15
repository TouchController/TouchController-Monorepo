package top.fifthlight.mergetools.test;

import top.fifthlight.mergetools.api.ActualConstructor
import top.fifthlight.mergetools.api.ActualImpl

@ActualImpl(TestInterfaceKt::class)
data class TestInterfaceKtImpl @ActualConstructor("of") constructor(
    override val age: Int,
    override val name: String,
) : TestInterfaceKt
