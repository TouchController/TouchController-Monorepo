package team.unnamed.mocha.runtime.value;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface EntityObjectProperty<T> extends ObjectProperty {
    @NotNull
    Value value(T entity);

    @NotNull
    default Value value() {
        return value(null);
    }

    // Cannot be constant
    default boolean constant() {
        return false;
    }
}
