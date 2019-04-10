package me.theeninja.primitivespecializer.core.processor;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedTypeVariable;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;

import java.util.Optional;
import java.util.stream.IntStream;

/**
 * The type specializer responsible for specializing type variables (such as {@code A}).
 */
public class DirectGenericTypeSpecializer extends TypeSpecializer<ResolvedTypeVariable, PrimitiveType> {
    DirectGenericTypeSpecializer(
        final NodeList<TypeParameter> annotatedElementTypeParameters,
        final JavaParserFacade javaParserFacade
    ) {
        super(annotatedElementTypeParameters, javaParserFacade);
    }

    @Override
    public Optional<PrimitiveType> getNewType(final ResolvedTypeVariable oldType, final PrimitiveTypesCombination primitiveTypesCombination) {
        for (int typeParameterIndex = 0; typeParameterIndex < getAnnotatedElementTypeParameters().size(); typeParameterIndex++) {
            final ResolvedTypeParameterDeclaration expectedTypeParameterDeclaration = getResolvedTypeParameterDeclaration(
                typeParameterIndex
            );

            if (areParameterArgumentPair(expectedTypeParameterDeclaration, oldType)) {
                final PrimitiveType[] primitiveTypes = primitiveTypesCombination.getPrimitiveTypes();
                final PrimitiveType primitiveType = primitiveTypes[typeParameterIndex];

                return Optional.ofNullable(primitiveType);
            }
        }

        return Optional.empty();
    }

    private boolean areParameterArgumentPair(
        final ResolvedTypeParameterDeclaration expectedTypeParameterDeclaration,
        final ResolvedTypeVariable observedTypeParameter
    ) {
        final String observedTypeParameterQualifiedName = observedTypeParameter.qualifiedName();
        final int observedTypeParameterSimpleNameIndex = observedTypeParameterQualifiedName.lastIndexOf('.') + 1;

        final String observedTypeParameterSimpleName = observedTypeParameterQualifiedName.substring(observedTypeParameterSimpleNameIndex);
        final String expectedTypeParameterSimpleName = expectedTypeParameterDeclaration.getName();

        return expectedTypeParameterSimpleName.equals(observedTypeParameterSimpleName);
    }
}
