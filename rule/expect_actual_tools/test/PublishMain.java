package top.fifthlight.mergetools.test;

public class PublishMain {
    public static void main(String[] args) {
        var impl = TestInterfaceFactory.of(18, "Bob");
        System.out.println("Age: " + impl.age());
        System.out.println("Name: " + impl.name());

        var ktImpl = TestInterfaceKtFactory.of(21, "Alice");
        System.out.println("Age: " + ktImpl.getAge());
        System.out.println("Name: " + ktImpl.getName());
    }
}
