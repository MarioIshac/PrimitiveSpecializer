package me.theeninja.primitivespecializer.core.annotation.classreplacement;

import java.util.function.Consumer;

public class ConsumerClassReplacement extends ClassReplacementDefault {
    @Override
    public Class<?> oldClass() {
        return Consumer.class;
    }
}
