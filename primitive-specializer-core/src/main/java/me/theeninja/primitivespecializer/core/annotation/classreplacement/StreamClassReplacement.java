package me.theeninja.primitivespecializer.core.annotation.classreplacement;

import me.theeninja.primitivespecializer.core.annotation.AnnotationPreProcessor;
import me.theeninja.primitivespecializer.core.annotation.FormattedClassSpecifier;

import java.util.stream.Stream;

public class StreamClassReplacement extends ClassReplacementDefault {
    @Override
    public Class<?> oldClass() {
        return Stream.class;
    }
}
