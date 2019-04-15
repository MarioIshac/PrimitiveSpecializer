package me.theeninja.primitivespecializer.core.processor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;
import lombok.Getter;
import me.theeninja.primitivespecializer.core.annotation.AnnotationPreProcessor;
import me.theeninja.primitivespecializer.core.annotation.ClassReplacement;
import me.theeninja.primitivespecializer.core.annotation.FormattedClassSpecifier;
import me.theeninja.primitivespecializer.core.annotation.ReplacementConfiguration;
import me.theeninja.primitivespecializer.core.annotation.classreplacement.*;
import me.theeninja.primitivespecializer.core.processor.aggregator.TypeArgumentsAggregator;

import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
abstract class KnownTypeSpecializer<A> extends TypeSpecializer<A, ClassOrInterfaceType> {
    private final ReplacementConfiguration replacementConfiguration;
    private final ClassToTypeMirror classToTypeMirror;
    private final DirectGenericTypeSpecializer directGenericTypeSpecializer;

    /**
     * These are all possible class replacements that have an {@link ClassReplacement#oldClass()} that exists in the java standard library and
     * a {@link ClassReplacement#formattedClassSpecifier()} which yields a new, specialized class in the java standard library.
     */
    private static final ClassReplacementDefault[] CLASS_REPLACEMENT_DEFAULTS = {
        new BiFunctionClassReplacement(),
        new BinaryOperatorClassReplacement(),
        new BufferClassReplacement(),
        new ConsumerClassReplacement(),
        new FunctionClassReplacement(),
        new OptionalClassReplacement(),
        new PredicateClassReplacement(),
        new SpliteratorClassReplacement(),
        new StreamClassReplacement(),
        new SupplierClassReplacement(),
        new UnaryOperatorClassReplacement()
    };

    abstract Iterable<? extends ResolvedType> getTypeArguments(A oldInformation);
    abstract TypeArgumentsAggregator newTypeArgumentsAggregator(PrimitiveTypesCombination primitiveTypesCombination);
    abstract String getQualifiedName(A oldInformation);

    KnownTypeSpecializer(
        final ReplacementConfiguration replacementConfiguration,
        final ClassToTypeMirror classToTypeMirror,
        final DirectGenericTypeSpecializer directGenericTypeSpecializer
    ) {
        super(
            directGenericTypeSpecializer.getAnnotatedElementTypeParameters(),
            directGenericTypeSpecializer.getJavaParserFacade()
        );

        this.replacementConfiguration = replacementConfiguration;
        this.classToTypeMirror = classToTypeMirror;
        this.directGenericTypeSpecializer = directGenericTypeSpecializer;
    }

    @Override
    Optional<ClassOrInterfaceType> getNewType(
        final A oldInformation,
        final PrimitiveTypesCombination primitiveTypesCombination
    ) {
        final ClassReplacement[] providedClassReplacements = getReplacementConfiguration().classReplacements();

        // There are the class replacements that are provided through the annotation
        final Stream<ClassReplacement> providedClassReplacementsStream = Arrays.stream(providedClassReplacements);

        // These are the class replacements that are for classes in the JRE, that need not be included in the annotation
        final Stream<ClassReplacement> defaultClassReplacementsStream = Arrays.stream(CLASS_REPLACEMENT_DEFAULTS);

        // We consider both groups of class replacements, hence we concat the pool of class replacements
        final Stream<ClassReplacement> classReplacementsStream = Stream.concat(
            providedClassReplacementsStream,
            defaultClassReplacementsStream
        );

        return findSpecializingType(classReplacementsStream, oldInformation, primitiveTypesCombination);
    }

    private Stream<TypeElement> getCoveredTypeElements(final ClassReplacement classReplacement) {
        final Stream.Builder<TypeElement> coveredTypeElements = Stream.builder();

        final DeclaredType oldDeclaredType = getClassToTypeMirror().convert(classReplacement::oldClass);
        final TypeElement oldTypeElement = (TypeElement) oldDeclaredType.asElement();

        coveredTypeElements.add(oldTypeElement);

        if (classReplacement.inherited()) {
            addEnclosedTypeElements(oldTypeElement, coveredTypeElements);
        }

        return coveredTypeElements.build();
    }

    private ClassOrInterfaceType getSpecializingType(
        final TypeElement equivalentOldTypeElement,
        final ClassReplacement classReplacement,
        final A oldInformation,
        final PrimitiveTypesCombination primitiveTypesCombination
    ) {
        final Iterable<? extends ResolvedType> typeArguments = getTypeArguments(oldInformation);

        final TypeArgumentsAggregator typeArgumentsAggregator = newTypeArgumentsAggregator(primitiveTypesCombination);

        typeArguments.forEach(typeArgumentsAggregator::aggregate);

        return getSpecializingType(classReplacement, equivalentOldTypeElement, typeArgumentsAggregator);
    }

    private ClassOrInterfaceType getSpecializingType(
        final ClassReplacement classReplacement,
        final TypeElement equivalentOldTypeElement,
        final TypeArgumentsAggregator typeArgumentsAggregator
    ) {
        final DeclaredType nonInheritedOldDeclaredType = getClassToTypeMirror().convert(classReplacement::oldClass);
        final TypeElement nonInheritedOldTypeElement = (TypeElement) nonInheritedOldDeclaredType.asElement();

        final String equivalentOldTypeElementQualifiedName = equivalentOldTypeElement.getQualifiedName().toString();

        final String oldTypeElementPackageName = AnnotationPreProcessor.getPackageName(equivalentOldTypeElement).toString();

        final int classNameStartIndex = oldTypeElementPackageName.length() + 1;

        final String oldTypeElementClassName = equivalentOldTypeElement.getQualifiedName().toString().substring(classNameStartIndex);

        final FormattedClassSpecifier formattedReplacingClassSpecifier = classReplacement.formattedClassSpecifier();

        final String newSimpleClassName = typeArgumentsAggregator.newSimpleClassName(formattedReplacingClassSpecifier, oldTypeElementClassName);
        final String newPackageName = AnnotationPreProcessor.getPreProcessedNewPackageName(formattedReplacingClassSpecifier, oldTypeElementPackageName);

        String newFullyQualifiedName = AnnotationPreProcessor.getFullyQualifiedName(newPackageName, newSimpleClassName);

        final Name nonInheritedOldTypeElementQualifiedName = nonInheritedOldTypeElement.getQualifiedName();

        final boolean specializingReplacementHasInheritedPart = nonInheritedOldTypeElementQualifiedName.contentEquals(
            equivalentOldTypeElementQualifiedName
        );

        /* Indicates that we must account for the non-inherited part not being included
           in ClassReplacement#formattedClassSpecifier()#classNameComponents(). */
        if (!specializingReplacementHasInheritedPart) {
            final int inheritedClassComponentIndex = nonInheritedOldTypeElementQualifiedName.length();

            final String inheritedClassComponent = equivalentOldTypeElementQualifiedName.substring(inheritedClassComponentIndex);

            newFullyQualifiedName += inheritedClassComponent;
        }

        final ClassOrInterfaceType specializingClass = JavaParser.parseClassOrInterfaceType(newFullyQualifiedName);

        NodeList<Type> newTypeArguments = typeArgumentsAggregator.getNewTypeArguments();

        // Prevents A<> from happening where A is a specialized class
        if (newTypeArguments.isNonEmpty()) {
            specializingClass.setTypeArguments(newTypeArguments);
        }

        return specializingClass;
    }

    private Function<TypeElement, ClassOrInterfaceType> newSpecializingTypeGetter(
        final ClassReplacement classReplacement,
        final A oldInformation,
        final PrimitiveTypesCombination primitiveTypesCombination
    ) {
        // This functor is guaranteed to be called with an `equivalentOldTypeElement` that is not null
        return equivalentOldTypeElement -> getSpecializingType(
            equivalentOldTypeElement,
            classReplacement,
            oldInformation,
            primitiveTypesCombination
        );
    }

    private boolean areEquivalent(A oldInformation, TypeElement typeElement) {
        final String expectedQualifiedName = getQualifiedName(oldInformation);
        final Name observedClassQualifiedName = typeElement.getQualifiedName();

        return observedClassQualifiedName.contentEquals(expectedQualifiedName);
    }

    private Function<ClassReplacement, Optional<ClassOrInterfaceType>> newSpecializedTypeFinder(
        final A oldInformation,
        final PrimitiveTypesCombination primitiveTypesCombination
    ) {
        return classReplacement -> getOptionalSpecializingType(classReplacement, oldInformation, primitiveTypesCombination);
    }

    private Optional<ClassOrInterfaceType> getOptionalSpecializingType(
        final ClassReplacement classReplacement,
        final A resolvedOldClass,
        final PrimitiveTypesCombination primitiveTypesCombination
    ) {
        final Optional<TypeElement> optionalCoveredOldTypeElement = findCoveredTypeElement(resolvedOldClass, classReplacement);

        final Function<TypeElement, ClassOrInterfaceType> specializingTypeGetter = newSpecializingTypeGetter(
                classReplacement,
                resolvedOldClass,
                primitiveTypesCombination
        );

        return optionalCoveredOldTypeElement.map(specializingTypeGetter);
    }

    private static final int MAX_CLASS_REPLACEMENTS_PER_CLASS = 1;

    private Optional<ClassOrInterfaceType> findSpecializingType(
        final Stream<ClassReplacement> classReplacementsStream,
        final A oldInformation,
        final PrimitiveTypesCombination primitiveTypesCombination
    ) {
        /* Given a class replacement, `specializedTypeFinder` will try to specialize the type based on the mapping covered by the class replacement.
        If the mapping is insufficient to specialize the old class, then an `specializedTypeFinder` returns an empty optional */
        final Function<ClassReplacement, Optional<ClassOrInterfaceType>> specializedTypeFinder = newSpecializedTypeFinder(oldInformation, primitiveTypesCombination);

        final List<ClassOrInterfaceType> specializedTypes = classReplacementsStream.map(specializedTypeFinder)
                .filter(Optional::isPresent)
                .map(Optional::get)
                // we limit to the expected number of class replacements plus 1 for error-checking later on
                .limit(MAX_CLASS_REPLACEMENTS_PER_CLASS + 1)
                .collect(Collectors.toUnmodifiableList());

        // This happens when there are no `ClassReplacement` annotations that have the old type required
        if (specializedTypes.isEmpty()) {
            return Optional.empty();
        }

        // This happens when there are multiple `ClassReplacement` annotations that have the same old type (and are in `classReplacementsStream`).
        if (specializedTypes.size() > MAX_CLASS_REPLACEMENTS_PER_CLASS) {
            // TODO throw compile time error
        }

        final ClassOrInterfaceType specializedType = specializedTypes.get(0);

        return Optional.of(specializedType);
    }

    /**
     * Here, a covered type element means a type element that represents a class that a class replacement covers.
     *
     * For example, in {@code @ClassReplacement(oldClass = Stream.class, inherited = false)}, the type elements that it covers are
     * the type element that represents {@code Stream}.
     *
     * @param oldInformation The class that has a type element equivalent to its raw type (since type elements represent declarations). This means that for
     *                      a resolved class of {@code Stream<Integer>}, the returned type element would represent {@code Stream}.
     * @param coveredTypeElements The candidates for the returned type element.
     *
     * @return A type element that is equivalent to {@code resolvedClass} in the manner described above that is also in {@code coveredTypeElements}.
     */
    private Optional<TypeElement> getEquivalentCoveredTypeElement(
        final A oldInformation,
        final Stream<TypeElement> coveredTypeElements
    ) {
        return coveredTypeElements.filter(typeElement -> areEquivalent(oldInformation, typeElement))
                                  .findFirst();
    }

    /**
     * Adds the type elements (to {@code coveredTypeElementsBuilder}) that represent subclasses of the class that {@code parentTypeElement} represents.
     */
    private void addEnclosedTypeElements(
        final TypeElement parentTypeElement,
        final Stream.Builder<TypeElement> coveredTypeElementsBuilder
    ) {
        for (final Element enclosedElement : parentTypeElement.getEnclosedElements()) {
            if (!(enclosedElement instanceof TypeElement)) {
                continue;
            }

            final TypeElement enclosedTypeElement = (TypeElement) enclosedElement;

            coveredTypeElementsBuilder.accept(enclosedTypeElement);

            addEnclosedTypeElements(enclosedTypeElement, coveredTypeElementsBuilder);
        }
    }
    private Optional<TypeElement> findCoveredTypeElement(
        final A oldInformation,
        final ClassReplacement classReplacement
    ) {
        final Stream<TypeElement> potentialEquivalentTypeElements = getCoveredTypeElements(classReplacement);

        return getEquivalentCoveredTypeElement(
            oldInformation,
            potentialEquivalentTypeElements
        );
    }
}
