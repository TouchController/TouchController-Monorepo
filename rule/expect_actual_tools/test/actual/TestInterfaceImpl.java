package top.fifthlight.mergetools.test;

import top.fifthlight.mergetools.api.ActualConstructor;
import top.fifthlight.mergetools.api.ActualImpl;

@ActualImpl(TestInterface.class)
public record TestInterfaceImpl(int age, String name) implements TestInterface {
    @ActualConstructor("of")
    public TestInterfaceImpl {
    }
}
