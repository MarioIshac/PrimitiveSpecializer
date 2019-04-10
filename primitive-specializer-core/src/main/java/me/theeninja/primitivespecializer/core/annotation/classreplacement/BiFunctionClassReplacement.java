package me.theeninja.primitivespecializer.core.annotation.classreplacement;

import java.util.function.BiFunction;

public class BiFunctionClassReplacement extends ClassReplacementDefault {
    @Override
    public Class<?> oldClass() {
        return BiFunction.class;
    }
}
