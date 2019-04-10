package me.theeninja.primitivespecializer.core.annotation.classreplacement;

import me.theeninja.primitivespecializer.core.annotation.ClassReplacement;
import me.theeninja.primitivespecializer.core.annotation.FormattedClassSpecifier;

import java.lang.annotation.Annotation;

public abstract class ClassReplacementDefault implements ClassReplacement {
    public static final boolean INHERITED_DEFAULT_VALUE = true;

    @Override
    public Class<? extends Annotation> annotationType() {
        return ClassReplacement.class;
    }

    @Override
    public boolean inherited() {
        return INHERITED_DEFAULT_VALUE;
    }

    @Override
    public FormattedClassSpecifier formattedClassSpecifier() {
        return new FormattedClassSpecifierDefault();
    }
}