package me.theeninja.primitivespecializer.core.processor.aggregator;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedTypeVariable;
import lombok.AccessLevel;
import lombok.Getter;
import me.theeninja.primitivespecializer.core.processor.DirectGenericTypeSpecializer;
import me.theeninja.primitivespecializer.core.processor.PrimitiveTypesCombination;

import java.util.Optional;

public class IndirectContextTypeArgumentsAggregator extends StaticContextTypeArgumentsAggregator {
    @Getter(AccessLevel.PUBLIC)
    private final NodeList<Type> newTypeArguments = new NodeList<>();

    public IndirectContextTypeArgumentsAggregator(final PrimitiveTypesCombination primitiveTypesCombination, final DirectGenericTypeSpecializer directGenericTypeSpecializer) {
        super(primitiveTypesCombination, directGenericTypeSpecializer);
    }

    @Override
    public void aggregate(final ResolvedType resolvedTypeArgument) {
        super.aggregate(resolvedTypeArgument);

        if (resolvedTypeArgument.isReferenceType()) {
            final ResolvedReferenceType resolvedReferenceType = resolvedTypeArgument.asReferenceType();

            aggregateReferenceArgument(resolvedReferenceType);
        }
    }

    @Override
    void aggregateTypeVariable(final ResolvedTypeVariable resolvedTypeVariable) {
        final Optional<PrimitiveType> optionalAssociatedPrimitiveType = getDirectGenericTypeSpecializer().getNewType(
            resolvedTypeVariable,
            getPrimitiveTypesCombination()
        );

        optionalAssociatedPrimitiveType.ifPresentOrElse(
            // Handles group 1
            this::aggregateSpecializedTypeVariable,

            // Handles group 3
            () -> aggregateTypeVariable(resolvedTypeVariable)
        );
    }

    private void aggregateReferenceArgument(final ResolvedReferenceType preservedResolvedReferenceType) {
        final String referenceTypeQualifiedName = preservedResolvedReferenceType.getQualifiedName();

        final ClassOrInterfaceType referenceType = JavaParser.parseClassOrInterfaceType(referenceTypeQualifiedName);

        this.getNewTypeArguments().add(referenceType);
    }
}
