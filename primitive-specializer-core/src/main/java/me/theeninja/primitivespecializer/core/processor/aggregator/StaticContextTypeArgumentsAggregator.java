package me.theeninja.primitivespecializer.core.processor.aggregator;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedTypeVariable;
import lombok.AccessLevel;
import lombok.Getter;
import me.theeninja.primitivespecializer.core.annotation.AnnotationPreProcessor;
import me.theeninja.primitivespecializer.core.annotation.FormattedClassSpecifier;
import me.theeninja.primitivespecializer.core.processor.DirectGenericTypeSpecializer;
import me.theeninja.primitivespecializer.core.processor.PrimitiveTypesCombination;

import java.util.Optional;

@Getter(AccessLevel.PACKAGE)
public class StaticContextTypeArgumentsAggregator extends TypeArgumentsAggregator {
    private final DirectGenericTypeSpecializer directGenericTypeSpecializer;
    private final NodeList<PrimitiveType> specializationsOfRemovedTypeArguments = new NodeList<>();

    public StaticContextTypeArgumentsAggregator(final PrimitiveTypesCombination primitiveTypesCombination, final DirectGenericTypeSpecializer directGenericTypeSpecializer) {
        super(primitiveTypesCombination);

        this.directGenericTypeSpecializer = directGenericTypeSpecializer;
    }

    @Override
    public void aggregate(final ResolvedType resolvedTypeArgument) {
        if (resolvedTypeArgument.isTypeVariable()) {
            ResolvedTypeVariable resolvedTypeVariable = resolvedTypeArgument.asTypeVariable();

            aggregateTypeVariable(resolvedTypeVariable);
        }
    }

    void aggregateTypeVariable(final ResolvedTypeVariable resolvedTypeVariable) {
        final Optional<PrimitiveType> optionalAssociatedPrimitiveType = getDirectGenericTypeSpecializer().getNewType(
            resolvedTypeVariable,
            getPrimitiveTypesCombination()
        );

        optionalAssociatedPrimitiveType.ifPresent(
            // Handles group 1
            this::aggregateSpecializedTypeVariable
        );
    }

    void aggregateSpecializedTypeVariable(final PrimitiveType specializationOfRemovedTypeArgument) {
        getSpecializationsOfRemovedTypeArguments().add(specializationOfRemovedTypeArgument);
    }

    @Override
    public String newSimpleClassName(FormattedClassSpecifier formattedClassSpecifier, String oldSimpleClassName) {
        PrimitiveType[] primitiveTypeArray = getSpecializationsOfRemovedTypeArguments().toArray(new PrimitiveType[0]);

        return AnnotationPreProcessor.getPreProcessedNewClassName(formattedClassSpecifier, oldSimpleClassName, primitiveTypeArray);
    }

    @Override
    public NodeList<Type> getNewTypeArguments() {
        return new NodeList<>();
    }
}
