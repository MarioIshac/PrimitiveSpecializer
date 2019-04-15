package me.theeninja.primitivespecializer.core.processor;

import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import lombok.Getter;
import me.theeninja.primitivespecializer.core.annotation.ReplacementConfiguration;
import me.theeninja.primitivespecializer.core.processor.aggregator.IndirectContextTypeArgumentsAggregator;
import me.theeninja.primitivespecializer.core.processor.aggregator.TypeArgumentsAggregator;

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
