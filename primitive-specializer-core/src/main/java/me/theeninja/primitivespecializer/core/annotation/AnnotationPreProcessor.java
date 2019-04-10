package me.theeninja.primitivespecializer.core.annotation;

import com.github.javaparser.ast.type.PrimitiveType;
import lombok.experimental.UtilityClass;
import me.theeninja.primitivespecializer.core.processor.SpecializationProcessor;

import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for pre processing annotation string values. Currently, those string values are:
 *
 * 1) Class Names (preprocessing example: "Map" supplied with "Int" and "Int" might yield "IntToIntMap").
 * 2) Package Names
 *
 * Preprocessing consists of two focuses:
 *
 * 1) Formatting the string with indirect arguments obtained through other values / source code.
 * 2) Substituting defaults (in place of default placeholders) determined during annotation-processing.
 *      For example, substituting the package name of the annotated class for the default placeholder "0".
 */
@UtilityClass
public class AnnotationPreProcessor {
    public final String SAME_PACKAGE = "0";

    public final String PRE_GENERIC_CLASS = "0";
    public final String POST_GENERIC_CLASS = "1";

    public final PrimitiveType GENERIC_PRIMITIVE_TYPE = null;

    public String getPreProcessedNewClassName(
        final FormattedClassSpecifier formattedClassSpecifier,
        final String genericClassName,
        final PrimitiveType[] primitiveTypes
    ) {
        final String[] newClassNameComponents = formattedClassSpecifier.classNameComponents();

        final PrimitiveReplacements primitiveReplacements = formattedClassSpecifier.primitiveReplacements();

        return preProcessProvidedNewClassName(
            genericClassName,
            newClassNameComponents,
            primitiveTypes,
            primitiveReplacements
        );
    }

    public Name getPackageName(final TypeElement enclosedTypeElement) {
        Element potentialPackageElement = enclosedTypeElement.getEnclosingElement();

        while (!(potentialPackageElement instanceof PackageElement)) {
            potentialPackageElement = potentialPackageElement.getEnclosingElement();
        }

        final PackageElement confirmedPackageElement = (PackageElement) potentialPackageElement;

        return confirmedPackageElement.getQualifiedName();
    }

    public String getPreProcessedFullyQualifiedName(
        final FormattedClassSpecifier formattedClassSpecifier,
        final String genericClassPackageName,
        final String genericClassName,
        final PrimitiveType[] primitiveTypes
    ) {
        final String preProcessedNewClassPackageName = getPreProcessedNewPackageName(
            formattedClassSpecifier,
            genericClassPackageName
        );

        final String preProcessedNewClassName = getPreProcessedNewClassName(
            formattedClassSpecifier,
            genericClassName,
            primitiveTypes
        );

        return getFullyQualifiedName(
            preProcessedNewClassPackageName,
            preProcessedNewClassName
        );
    }

    public String getFullyQualifiedName(final String newClassPackageName, final String newClassName) {
        final StringBuilder fullyQualifiedNameBuilder = new StringBuilder();

        // If not default package
        if (!newClassPackageName.equals("")) {
            fullyQualifiedNameBuilder.append(newClassPackageName).append(".");
        }

        fullyQualifiedNameBuilder.append(newClassName);

        return fullyQualifiedNameBuilder.toString();
    }

    public String getPreProcessedNewPackageName(
            final FormattedClassSpecifier formattedClassSpecifier,
            final String genericClassPackageName
    ) {
        final String newClassPackageName = formattedClassSpecifier.packageName();

        return preProcessProvidedNewPackageName(genericClassPackageName, newClassPackageName);
    }

    private String getPrimitiveTypePlaceholderReplacement(
        final PrimitiveType primitiveType,
        final PrimitiveReplacements primitiveReplacements
    ) {
        final PrimitiveType.Primitive primitive = primitiveType.getType();

        switch (primitive) {
            case BOOLEAN: return primitiveReplacements.forBoolean();
            case BYTE: return primitiveReplacements.forByte();
            case CHAR: return primitiveReplacements.forChar();
            case SHORT: return primitiveReplacements.forShort();
            case INT: return primitiveReplacements.forInt();
            case LONG: return primitiveReplacements.forLong();
            case FLOAT: return primitiveReplacements.forFloat();
            case DOUBLE: return primitiveReplacements.forDouble();
            default: throw new AssertionError(SpecializationProcessor.NOT_ALL_PRIMITIVES_HANDLED);
        }
    }

    private String getJoinedPrimitiveTypes(
        final PrimitiveType[] primitiveTypes,
        final PrimitiveReplacements primitiveReplacements
    ) {
        final StringBuilder joinedPrimitiveTypesBuilder = new StringBuilder();

        for (PrimitiveType primitiveType : primitiveTypes) {
            final String primitiveTypePlaceholderReplacement = getPrimitiveTypePlaceholderReplacement(
                primitiveType,
                primitiveReplacements
            );

            joinedPrimitiveTypesBuilder.append(primitiveTypePlaceholderReplacement);
        }

        return joinedPrimitiveTypesBuilder.toString();
    }

    private final int DEFAULT_CLASS_NAME_COMPONENTS_INDEX = 0;

    private final String PRIMITIVE_TYPE_PLACEHOLDER_START = "<";
    private final String PRIMITIVE_TYPE_PLACEHOLDER_END = ">";

    private String newIndexedPlaceholder(final int typeArgumentIndex) {
        return PRIMITIVE_TYPE_PLACEHOLDER_START + typeArgumentIndex + PRIMITIVE_TYPE_PLACEHOLDER_END;
    }

    private final Pattern PRIMITIVE_TYPE_PLACEHOLDER_PATTERN = Pattern.compile(
        PRIMITIVE_TYPE_PLACEHOLDER_START + "(.*?)" + PRIMITIVE_TYPE_PLACEHOLDER_END
    );

    private final String NO_PRIMITIVE_TYPE_REPLACEMENT_FOUND = "";

    private Function<String, String> newClassNameComponentFormatter(
        final PrimitiveType[] primitiveTypes,
        final PrimitiveReplacements primitiveReplacements
    ) {
        return toBeFormattedClassNameComponent -> {
            Matcher matcher = PRIMITIVE_TYPE_PLACEHOLDER_PATTERN.matcher(toBeFormattedClassNameComponent);

            while (matcher.find()) {
                final String typeParameterIndexAsString = matcher.group(1);
                final int typeParameterIndex = Integer.parseInt(typeParameterIndexAsString);

                if (!(0 <= typeParameterIndex && typeParameterIndex < primitiveTypes.length)) {
                    return NO_PRIMITIVE_TYPE_REPLACEMENT_FOUND;
                }

                final PrimitiveType primitiveType = primitiveTypes[typeParameterIndex];

                if (primitiveType == null) {
                    toBeFormattedClassNameComponent = NO_PRIMITIVE_TYPE_REPLACEMENT_FOUND;
                }
                else {
                    String primitiveTypePlaceholder = newIndexedPlaceholder(typeParameterIndex);

                    final String primitiveTypeRepresentation = getPrimitiveTypePlaceholderReplacement(
                            primitiveType,
                            primitiveReplacements
                    );

                    toBeFormattedClassNameComponent = toBeFormattedClassNameComponent.replace(
                            primitiveTypePlaceholder,
                            primitiveTypeRepresentation
                    );
                }
            }

            return toBeFormattedClassNameComponent;
        };
    }

    private String preProcessProvidedNewClassName(
        final String annotatedClassName,
        final String[] providedNewClassNameComponents,
        final PrimitiveType[] primitiveTypes,
        final PrimitiveReplacements primitiveReplacements
    ) {
        final String potentialDefaultClassNameComponents = providedNewClassNameComponents[DEFAULT_CLASS_NAME_COMPONENTS_INDEX];

        switch (potentialDefaultClassNameComponents) {
            case PRE_GENERIC_CLASS: {
                final String joinedPrimitiveTypeNames = getJoinedPrimitiveTypes(primitiveTypes, primitiveReplacements);

                return joinedPrimitiveTypeNames + annotatedClassName;
            }
            case POST_GENERIC_CLASS: {
                final String joinedPrimitiveTypeNames = getJoinedPrimitiveTypes(primitiveTypes, primitiveReplacements);

                return annotatedClassName + joinedPrimitiveTypeNames;
            }
            default: {
                final Function<String, String> classNameComponentFormatter = newClassNameComponentFormatter(primitiveTypes, primitiveReplacements);

                String formattedClassName = Arrays.stream(providedNewClassNameComponents)
                                                  .map(classNameComponentFormatter)
                                                  .collect(Collectors.joining());

                System.out.println("Formatted class name " + formattedClassName);

                return formattedClassName;
            }
        }
    }

    private String preProcessProvidedNewPackageName(final String annotatedClassPackageName,
                                                    final String providedGeneratedPackageName) {
        if (AnnotationPreProcessor.SAME_PACKAGE.equals(providedGeneratedPackageName)) {
            return annotatedClassPackageName;
        }

        return providedGeneratedPackageName;
    }
}
