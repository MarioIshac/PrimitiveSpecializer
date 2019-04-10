package me.theeninja.primitivespecializer.core.annotation.classreplacement;

import java.util.function.BinaryOperator;

public class BinaryOperatorClassReplacement extends ClassReplacementDefault {
    @Override
    public Class<?> oldClass() {
        return BinaryOperator.class;
    }
}
