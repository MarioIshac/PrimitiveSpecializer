package me.theeninja.primitivespecializer.core.processor;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.symbolsolver.core.resolution.Context;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFactory;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

@AllArgsConstructor
@Getter
abstract class TypeSpecializer<A, R extends Type> {
    private final NodeList<TypeParameter> annotatedElementTypeParameters;
    private final JavaParserFacade javaParserFacade;

    /**
     * @param oldType The type to be replaced.
     * @param primitiveTypesCombination The factors that determine the new type.
     * @return An optional replacement for {@code oldType}.
     */
    abstract Optional<R> getNewType(final A oldType, final PrimitiveTypesCombination primitiveTypesCombination);

    ResolvedTypeParameterDeclaration getResolvedTypeParameterDeclaration(final int typeParameterIndex) {
        final TypeParameter typeParameter = getAnnotatedElementTypeParameters().get(typeParameterIndex);

        final TypeSolver javaParserFascadeTypeSolver = getJavaParserFacade().getTypeSolver();

        /* JavaParserFascade provides a `convertToUsage` method that only works for `ClassOrInterfaceType`. If the type parameter was
        previously represented by a `ClassOrInterfaceType`, an unsolved symbol exception is still thrown. Thus, we do it manually: */
        final Context expectedTypeParameterContext = JavaParserFactory.getContext(
            typeParameter,
            javaParserFascadeTypeSolver
        );

        final String expectedTypeParameterName = typeParameter.getNameAsString();

        final SymbolReference<ResolvedTypeDeclaration> expectedTypeParameterSymbolReference = expectedTypeParameterContext.solveType(
            expectedTypeParameterName
        );

        // This is guaranteed to ahve a defined type of `ResolvedTypeParameterDeclaration`
        final ResolvedTypeDeclaration expectedTypeDeclarationOfParameter = expectedTypeParameterSymbolReference.getCorrespondingDeclaration();

        return (ResolvedTypeParameterDeclaration) expectedTypeDeclarationOfParameter;
    }
}
