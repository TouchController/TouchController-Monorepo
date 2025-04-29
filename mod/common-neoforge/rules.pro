-dontobfuscate
-dontoptimize

-dontwarn android.annotation.*

-keep class top.fifthlight.touchcontroller.TouchController
-keep class top.fifthlight.touchcontroller.mixin.* { *; }
-keep class top.fifthlight.combine.*.CombineScreen { *; }
-keep class top.fifthlight.touchcontroller.common.platform.win32.Interface { *; }
-keep class top.fifthlight.touchcontroller.common.platform.wayland.Interface { *; }
-keep class top.fifthlight.touchcontroller.common.platform.android.Transport { *; }
-keep @net.neoforged.fml.common.Mod class * { *; }
-keepclassmembers class * {
    @net.neoforged.bus.api.SubscribeEvent *;
}

-keeppackagenames top.fifthlight.touchcontroller.**
-repackageclasses top.fifthlight.touchcontroller.relocated

-allowaccessmodification

-keepattributes Signature,Exceptions,*Annotation*,InnerClasses,PermittedSubclasses,EnclosingMethod,Deprecated,SourceFile,LineNumberTable
