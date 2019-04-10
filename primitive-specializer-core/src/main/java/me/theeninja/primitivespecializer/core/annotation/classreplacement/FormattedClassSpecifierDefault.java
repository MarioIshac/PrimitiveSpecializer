package me.theeninja.primitivespecializer.core.annotation.classreplacement;

import me.theeninja.primitivespecializer.core.annotation.AnnotationPreProcessor;
import me.theeninja.primitivespecializer.core.annotation.FormattedClassSpecifier;
import me.theeninja.primitivespecializer.core.annotation.PrimitiveReplacements;

import java.lang.annotation.Annotation;

public class FormattedClassSpecifierDefault implements FormattedClassSpecifier {
    public static final String PACKAGE_DEFAULT_VALUE = AnnotationPreProcessor.SAME_PACKAGE;
    public static final String CLASS_NAME_COMPONENTS_DEFAULT_VALUE = AnnotationPreProcessor.PRE_GENERIC_CLASS;

    @Override
    public Class<? extends Annotation> annotationType() {
        return FormattedClassSpecifier.class;
    }

    @Override
    public String packageName() {
        return PACKAGE_DEFAULT_VALUE;
    }

    @Override
    public String[] classNameComponents() {
        return new String[] {
            CLASS_NAME_COMPONENTS_DEFAULT_VALUE
        };
    }

    @Override
    public PrimitiveReplacements primitiveReplacements() {
        return new PrimitiveReplacementsDefault();
    }
}
