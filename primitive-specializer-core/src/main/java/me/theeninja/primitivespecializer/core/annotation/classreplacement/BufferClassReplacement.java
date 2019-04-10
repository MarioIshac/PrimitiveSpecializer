package me.theeninja.primitivespecializer.core.annotation.classreplacement;

import java.nio.Buffer;

public class BufferClassReplacement extends ClassReplacementDefault {
    @Override
    public Class<?> oldClass() {
        return Buffer.class;
    }
}
