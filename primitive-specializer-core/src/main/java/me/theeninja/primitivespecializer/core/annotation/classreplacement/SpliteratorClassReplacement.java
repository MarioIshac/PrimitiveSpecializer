package me.theeninja.primitivespecializer.core.annotation.classreplacement;

import me.theeninja.primitivespecializer.core.annotation.FormattedClassSpecifier;

import java.util.Spliterator;

public class SpliteratorClassReplacement extends ClassReplacementDefault {
    @Override
    public Class<?> oldClass() {
        return Spliterator.class;
    }

    @Override
    public FormattedClassSpecifier formattedClassSpecifier() {
        return new FormattedClassSpecifierDefault() {
            @Override
            public String[] classNameComponents() {
                return new String[] {
                    oldClass().getSimpleName(), "Of{0}"
                };
            }
        };
    }
}
