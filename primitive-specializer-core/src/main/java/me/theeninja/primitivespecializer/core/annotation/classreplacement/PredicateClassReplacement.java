package me.theeninja.primitivespecializer.core.annotation.classreplacement;

import me.theeninja.primitivespecializer.core.annotation.AnnotationPreProcessor;
import me.theeninja.primitivespecializer.core.annotation.FormattedClassSpecifier;

import java.util.function.Predicate;

public class PredicateClassReplacement extends ClassReplacementDefault {
    @Override
    public Class<?> oldClass() {
        return Predicate.class;
    }
}
