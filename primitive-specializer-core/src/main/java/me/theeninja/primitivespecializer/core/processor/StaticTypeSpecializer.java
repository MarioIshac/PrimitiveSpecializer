package me.theeninja.primitivespecializer.core.processor;

import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.parametrization.ResolvedTypeParametersMap;
import me.theeninja.primitivespecializer.core.annotation.ReplacementConfiguration;
import me.theeninja.primitivespecializer.core.processor.aggregator.StaticContextTypeArgumentsAggregator;
import me.theeninja.primitivespecializer.core.processor.aggregator.TypeArgumentsAggregator;

import java.util.List;

/**
 * The type specializer responsible for specializing the use of parametrized classes in static contexts, such
 * as {@code List} in {@link List#add(Object)}.
 */
class StaticTypeSpecializer extends KnownTypeSpecializer<MethodUsage> {
    StaticTypeSpecializer(
        final ReplacementConfiguration replacementConfiguration,
        final ClassToTypeMirror classToTypeMirror,
        final DirectGenericTypeSpecializer directGenericTypeSpecializer
    ) {
        super(replacementConfiguration, classToTypeMirror, directGenericTypeSpecializer);
    }

    @Override
    Iterable<? extends ResolvedType> getTypeArguments(final MethodUsage methodUsage) {
        final ResolvedTypeParametersMap resolvedTypeParametersMap = methodUsage.typeParametersMap();

        System.out.println("Static resolved type parameters map " + resolvedTypeParametersMap);

        return resolvedTypeParametersMap.getTypes();
    }

    @Override
    TypeArgumentsAggregator newTypeArgumentsAggregator(final PrimitiveTypesCombination primitiveTypesCombination) {
        return new StaticContextTypeArgumentsAggregator(primitiveTypesCombination, getDirectGenericTypeSpecializer());
    }

    @Override
    String getQualifiedName(final MethodUsage staticContextUse) {
        final ResolvedTypeDeclaration resolvedTypeDeclaration = staticContextUse.declaringType();

        return resolvedTypeDeclaration.getQualifiedName();
    }
}
