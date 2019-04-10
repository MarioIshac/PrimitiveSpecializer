package me.theeninja.primitivespecializer.core.processor;

import com.github.javaparser.ast.type.PrimitiveType;
import lombok.Getter;
import me.theeninja.primitivespecializer.core.annotation.AnnotationPreProcessor;
import me.theeninja.primitivespecializer.core.annotation.FormattedClassSpecifier;

@Getter
public class PrimitiveTypesCombination {
    private final PrimitiveType[] primitiveTypes;
    private final String preProcessedSpecializationClass;

    PrimitiveTypesCombination(final PrimitiveType[] primitiveTypes,
                              final String annotatedClassName,
                              final FormattedClassSpecifier formattedClassSpecifier) {
        this.primitiveTypes = primitiveTypes;

        this.preProcessedSpecializationClass = AnnotationPreProcessor.getPreProcessedNewClassName(
            formattedClassSpecifier,
            annotatedClassName,
            getPrimitiveTypes()
        );
    }
}
