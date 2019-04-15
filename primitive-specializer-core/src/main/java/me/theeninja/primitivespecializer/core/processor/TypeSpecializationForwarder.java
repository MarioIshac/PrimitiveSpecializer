package me.theeninja.primitivespecializer.core.processor;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedTypeVariable;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import lombok.AccessLevel;
import lombok.Getter;
import me.theeninja.primitivespecializer.core.annotation.ReplacementConfiguration;

import java.util.Optional;

@Getter(AccessLevel.PACKAGE)
public class TypeSpecializationForwarder {
    private final DirectGenericTypeSpecializer directGenericTypeSpecializer;
    private final IndirectGenericTypeSpecializer indirectGenericTypeSpecializer;

    @Getter(AccessLevel.PUBLIC)
    private final StaticTypeSpecializer staticTypeSpecializer;

    private final JavaParserFacade javaParserFacade;

    /**
     * @param specializationVisitor The specialization visitor that holds the arguments that will be passed into all
     *                              the various subclasses of {@link TypeSpecializer}.
     */
    public TypeSpecializationForwarder(final SpecializationVisitor specializationVisitor) {
        final NodeList<TypeParameter> annotatedElementTypeParameters = specializationVisitor.getAnnotatedElementTypeParameters();
        final ReplacementConfiguration replacementConfiguration = specializationVisitor.getReplacementConfiguration();
        final ClassToTypeMirror classToTypeMirror = specializationVisitor.getClassToTypeMirror();

        this.javaParserFacade = specializationVisitor.getJavaParserFacade();

        this.directGenericTypeSpecializer = new DirectGenericTypeSpecializer(annotatedElementTypeParameters, getJavaParserFacade());

        this.indirectGenericTypeSpecializer = new IndirectGenericTypeSpecializer(
            replacementConfiguration,
            classToTypeMirror,
            getDirectGenericTypeSpecializer()
        );

        this.staticTypeSpecializer = new StaticTypeSpecializer(replacementConfiguration, classToTypeMirror, getDirectGenericTypeSpecializer());
    }

    /**
     * @param oldType The old type.
     * @param primitiveTypesCombination The combination of primitive types responsible for determining the new type from the old type.
     * @return The new type.
     */
    private Type getUpdatedVariableType(Type oldType, PrimitiveTypesCombination primitiveTypesCombination) {
        final ResolvedType resolvedOldType = getJavaParserFacade().convertToUsage(oldType);

        final Optional<? extends Type> optionalNewType = getOptionalNewType(resolvedOldType, primitiveTypesCombination);

        return optionalNewType.isPresent() ? optionalNewType.get() : oldType;
    }



    /**
     * Sets a new type for {@code nodeWithType} based on its old type.
     *
     * @param nodeWithType The node which has an associated type. The current type serves as the old type of this node.
     * @param primitiveTypesCombination The combination of primitive types responsible for determining the new type from the old type.
     */
    public void updateVariableType(final NodeWithType<?, Type> nodeWithType, final PrimitiveTypesCombination primitiveTypesCombination) {
        final Type oldType = nodeWithType.getType();
        final Type newType = getUpdatedVariableType(oldType, primitiveTypesCombination);
        nodeWithType.setType(newType);
    }

    /**
     * @param resolvedOldType The resolved old type.
     * @param primitiveTypesCombination The combination of primitive types responsible for determining the new type from the resolved old type.
     * @return A new type if the old type is still insufficient given {@code primitiveTypesCombination}. If the old type is still sufficient, then an empty
     *         optional is returned.
     */
    private Optional<? extends Type> getOptionalNewType(final ResolvedType resolvedOldType, final PrimitiveTypesCombination primitiveTypesCombination) {
        if (resolvedOldType.isTypeVariable()) {
            final ResolvedTypeVariable resolvedTypeVariable = resolvedOldType.asTypeVariable();

            return getDirectGenericTypeSpecializer().getNewType(resolvedTypeVariable, primitiveTypesCombination);
        }

        if (resolvedOldType.isReferenceType()) {
            final ResolvedReferenceType resolvedReferenceType = resolvedOldType.asReferenceType();

            return getIndirectGenericTypeSpecializer().getNewType(resolvedReferenceType, primitiveTypesCombination);
        }

        return Optional.empty();
    }
}
