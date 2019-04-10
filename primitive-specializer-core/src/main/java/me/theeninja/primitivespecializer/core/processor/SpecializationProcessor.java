package me.theeninja.primitivespecializer.core.processor;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.java.Log;
import me.theeninja.primitivespecializer.core.annotation.FormattedClassSpecifier;
import me.theeninja.primitivespecializer.core.annotation.PrimitiveSpecialization;
import me.theeninja.primitivespecializer.core.annotation.PrimitiveSpecializations;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log
public class SpecializationProcessor extends AbstractProcessor {
    public static final String NOT_ALL_PRIMITIVES_HANDLED = "Method does not handle all possible primitives";

    private static final Set<String> SUPPORTED_ANNOTATION_TYPES = Stream.of(PrimitiveSpecializations.class)
                                                                        .map(Class::getName)
                                                                        .collect(Collectors.toSet());

    /**
     * @param formatErrorMessage The error message with placeholders for {@code formatParameters}, for example:
     *                           "Cannot cast %s to %s" (assuming {@code formatParameters.length} is 2).
     * @param formatParameters The parameters that will replace the placeholders in {@code formatErrorMessage}, based on
     *                         the string returned by calling {@code toString} on each one of them.
     */
    private void error(final String formatErrorMessage, final Object... formatParameters) {
        final Messager messager = processingEnv.getMessager();
        final String errorMessage = String.format(formatErrorMessage, formatParameters);

        messager.printMessage(Diagnostic.Kind.ERROR, errorMessage);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return SUPPORTED_ANNOTATION_TYPES;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private static int getObservedTypeParameterCount(final String[] formatClassNameComponents) {
        int observedTypeParameterCount = 0;

        for (String formatClassNameComponent : formatClassNameComponents) {
            if (formatClassNameComponent.contains("%s")) {
                observedTypeParameterCount++;
            }
        }

        return observedTypeParameterCount;
    }


    /**
     * @param primitiveSpecialization The annotation that will be target of this implicit-genericness test.
     * @return Whether the annotation is implicitly generic (meaning that for any primitive type,
     * {@code primitiveSpecialization.for{primitiveType}()}, for example {@code primitiveSpecialization.forChar()}
     *        is false.
     */
    private static boolean isGenericImplicitly(final PrimitiveSpecialization primitiveSpecialization) {
        return !(primitiveSpecialization.forByte()     ||
                 primitiveSpecialization.forChar()     ||
                 primitiveSpecialization.forShort()    ||
                 primitiveSpecialization.forInt()      ||
                 primitiveSpecialization.forLong()     ||
                 primitiveSpecialization.forFloat()    ||
                 primitiveSpecialization.forDouble());
    }

    /**
     * @param primitiveSpecialization The annotation that will be the target of this genericness test.
     * @return Whether the annotation is implicitly generic or explicitly generic (can be both).
     */
    private static boolean isGeneric(final PrimitiveSpecialization primitiveSpecialization) {
        return primitiveSpecialization.forGeneric() || isGenericImplicitly(primitiveSpecialization);
    }

    private static final Set<String> ALLOWED_TYPE_PARAMETER_BOUNDS = Stream.of(
        Object.class, // Object#hashCode() = Primitive#modulus(), Object#equals(Object) = Primitive == Primitive
        Number.class, // Number#[primitiveName]Value() = (primitiveName) Primitive
        Cloneable.class, // Object#clone() = Primitive /* primitives are copied by value */
        Serializable.class, //
        Comparable.class // Object.compare(Object) = Primitive < Primitive
    ).map(Class::getName).collect(Collectors.toUnmodifiableSet());

    @AllArgsConstructor
    @Getter(AccessLevel.PRIVATE)
    private enum TypeParameterType {
        GENERIC_ALLOWED(true, false),
        GENERIC_DISALLOWED(false, false),
        SPECIALIZED(true, true);

        private boolean isAllowed;
        private boolean isPlaceholderSubstitutionRequired; 
    }

    private TypeParameterType getTypeParameterType(final TypeParameterElement typeParameterElement, final PrimitiveSpecialization primitiveSpecialization) {
        // Being generic (meaning this specific type parameter will not be specialized) overrides all else
        final boolean isGeneric = isGeneric(primitiveSpecialization);

        if (isGeneric) {
            final List<? extends TypeMirror> typeParameterBounds = typeParameterElement.getBounds();

            for (final TypeMirror typeParameterBound : typeParameterBounds) {
                final String typeParameterBoundName = typeParameterBound.toString();

                final boolean typeParamterBoundAllowed = ALLOWED_TYPE_PARAMETER_BOUNDS.contains(typeParameterBoundName);

                if (!typeParamterBoundAllowed) {
                    final String allowedTypeParameterBoundNames = String.join(", ", ALLOWED_TYPE_PARAMETER_BOUNDS);

                    error(
                        "%s as a type parameter bound is not allowed, must be in %s",
                        typeParameterBoundName,
                        allowedTypeParameterBoundNames
                    );

                    return TypeParameterType.GENERIC_DISALLOWED;
                }
            }

            return TypeParameterType.GENERIC_ALLOWED;
        }

        return TypeParameterType.SPECIALIZED;
    }

    /**
     * @param annotatedElement the element annotated with {@link PrimitiveSpecializations}.
     * @return whether processing of the annotation was successful
     *        (and processing for other annotated elements should continue).
     */
    private boolean processSingle(final TypeElement annotatedElement) {
        final List<? extends TypeParameterElement> typeParameterElements = annotatedElement.getTypeParameters();

        final PrimitiveSpecializations primitiveSpecializations = annotatedElement.getAnnotation(PrimitiveSpecializations.class);
        final PrimitiveSpecialization[] primitiveSpecializationArray = primitiveSpecializations.value();

        final FormattedClassSpecifier primitiveSpecializationClassSpecifier = primitiveSpecializations.formattedClassSpecifier();
        final String[] formatSpecializationClassNameComponents  = primitiveSpecializationClassSpecifier.classNameComponents();

        final int formatStringBasedSpecializedParameterCount = getObservedTypeParameterCount(
            formatSpecializationClassNameComponents
        );

        if (primitiveSpecializationArray.length != 0) {
            int annotationValueBasedSpecializedParamterCount = 0;

            for (int typeParameterIndex = 0; typeParameterIndex < primitiveSpecializationArray.length; typeParameterIndex++) {
                final PrimitiveSpecialization primitiveSpecialization = primitiveSpecializationArray[typeParameterIndex];
                final TypeParameterElement typeParameter = typeParameterElements.get(typeParameterIndex);

                final TypeParameterType typeParameterTypeInContext = getTypeParameterType(typeParameter, primitiveSpecialization);

                if (!typeParameterTypeInContext.isAllowed()) {
                    return false;
                }

                if (typeParameterTypeInContext.isPlaceholderSubstitutionRequired()) {
                    annotationValueBasedSpecializedParamterCount++;
                }
            }

            if (formatStringBasedSpecializedParameterCount != annotationValueBasedSpecializedParamterCount) {
                error(
                    "format string for new primitive-specialized class names expects %i parameters," +
                    "but there are %i non-None me.theeninja.me.theeninja.primitivespecializer.core.annotation.PrimitiveSpecialization values specified",
                    formatStringBasedSpecializedParameterCount,
                    annotationValueBasedSpecializedParamterCount
                );

                return false;
            }
        }

        final int maxTypeParameterCount = typeParameterElements.size();

        if (formatStringBasedSpecializedParameterCount > maxTypeParameterCount) {
            error(
                "format string for new primitive-specialized class names expects %i parameters," +
                "which is more than the maximum number able to be provided (%i)",
                formatStringBasedSpecializedParameterCount,
                maxTypeParameterCount
            );

            return false;
        }

        final Filer filer = processingEnv.getFiler();
        final Elements elementUtils = processingEnv.getElementUtils();

        final ClassToTypeMirror classToTypeMirror = new ClassToTypeMirror(elementUtils);

        final SpecializationWriter specializationWriter = new SpecializationWriter(
            filer,
            annotatedElement,
            classToTypeMirror
        );

        specializationWriter.generateSpecializationPerCombination(primitiveSpecializations, typeParameterElements.size());

        return true;
    }

    @Override
    public boolean process(final Set<? extends TypeElement> typeElements, final RoundEnvironment roundEnvironment) {
        final Set<? extends Element> annotatedElements = roundEnvironment.getElementsAnnotatedWith(PrimitiveSpecializations.class);

        log.info(String.format("Processing %s @PrimitivesSpecialization-elements", annotatedElements.size()));

        for (final Element annotatedElement : annotatedElements) {
            TypeElement typeElement = (TypeElement) annotatedElement;
            final boolean processingResult = processSingle(typeElement);

            log.info(String.format("Processed %s with result %s", typeElement, processingResult));

            if (!processingResult) {
                return false;
            }
        }

        return true;
    }
}
