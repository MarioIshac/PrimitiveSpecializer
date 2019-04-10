package me.theeninja.primitivespecializer.core.processor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.utils.Pair;
import lombok.Getter;
import me.theeninja.primitivespecializer.core.annotation.AnnotationPreProcessor;
import me.theeninja.primitivespecializer.core.annotation.ClassReplacement;
import me.theeninja.primitivespecializer.core.annotation.FormattedClassSpecifier;
import me.theeninja.primitivespecializer.core.annotation.ReplacementConfiguration;
import me.theeninja.primitivespecializer.core.processor.aggregator.IndirectContextTypeArgumentsAggregator;
import me.theeninja.primitivespecializer.core.processor.aggregator.TypeArgumentsAggregator;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The type specializer responsible for specializing generic reference types (such as {@code List<A>}).
 */
@Getter
class IndirectGenericTypeSpecializer extends KnownTypeSpecializer<ResolvedReferenceType> {
    @Override
    Iterable<? extends ResolvedType> getTypeArguments(final ResolvedReferenceType oldInformation) {
        return oldInformation.getTypeParametersMap().stream().map(typeParameterArgumentPair -> typeParameterArgumentPair.b).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    TypeArgumentsAggregator newTypeArgumentsAggregator(final PrimitiveTypesCombination primitiveTypesCombination) {
        return new IndirectContextTypeArgumentsAggregator(primitiveTypesCombination, getDirectGenericTypeSpecializer());
    }

    IndirectGenericTypeSpecializer(
        final ReplacementConfiguration replacementConfiguration,
        final ClassToTypeMirror classToTypeMirror,
        final DirectGenericTypeSpecializer directGenericTypeSpecializer
    ) {
        super(replacementConfiguration, classToTypeMirror, directGenericTypeSpecializer);
    }

    @Override
    String getQualifiedName(ResolvedReferenceType oldInformation) {
        return oldInformation.getQualifiedName();
    }
}
