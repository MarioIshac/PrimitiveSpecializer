package me.theeninja.primitivespecializer.core.annotation.classreplacement;

import me.theeninja.primitivespecializer.core.annotation.AnnotationPreProcessor;
import me.theeninja.primitivespecializer.core.annotation.FormattedClassSpecifier;

import java.util.Optional;

public class OptionalClassReplacement extends ClassReplacementDefault {
    @Override
    public Class<?> oldClass() {
        return Optional.class;
    }

    @Override
    public FormattedClassSpecifier formattedClassSpecifier() {
        return new FormattedClassSpecifierDefault() {
            @Override
            public String[] classNameComponents() {
                return new String[] {
                    AnnotationPreProcessor.POST_GENERIC_CLASS
                };
            }
        }; 
    }
}
