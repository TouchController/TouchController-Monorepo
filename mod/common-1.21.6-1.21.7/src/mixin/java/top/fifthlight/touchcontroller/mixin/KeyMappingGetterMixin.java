package top.fifthlight.touchcontroller.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(KeyMapping.class)
public interface KeyMappingGetterMixin {
    @Accessor("ALL")
    static Map<String, KeyMapping> touchcontroller$getAllKeyMappings() {
        throw new AssertionError();
    }
}