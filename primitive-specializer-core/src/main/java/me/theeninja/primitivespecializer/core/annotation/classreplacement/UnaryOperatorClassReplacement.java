package me.theeninja.primitivespecializer.core.annotation.classreplacement;

import java.util.function.IntUnaryOperator;
import java.util.function.UnaryOperator;

public class UnaryOperatorClassReplacement extends ClassReplacementDefault {
    @Override
    public Class<?> oldClass() {
        return UnaryOperator.class;
    }
}
