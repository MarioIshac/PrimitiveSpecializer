package me.theeninja.primitivespecializer.core.annotation.classreplacement;

import java.util.function.Function;

public class FunctionClassReplacement extends ClassReplacementDefault {
    @Override
    public Class<?> oldClass() {
        return Function.class;
    }
}
