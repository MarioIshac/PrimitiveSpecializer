package me.theeninja.primitivespecializer.core.processor;

import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import lombok.AccessLevel;
import lombok.Getter;
import me.theeninja.primitivespecializer.core.annotation.*;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.type.PrimitiveType;

import javax.annotation.processing.Filer;
import javax.lang.model.element.*;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

@Getter(AccessLevel.PRIVATE)
class SpecializationWriter {
    private final Filer filer;
    private final SourceInformation annotatedClassSourceInformation;
    private final ClassToTypeMirror classToTypeMirror;
    private final TypeElement annotatedTypeElement;
    private final TypeSolver typeSolver;

    private static class SourceInformation {
        private final String sourceCode;
        private final String sourceDirectory;

        private SourceInformation(String sourceCode, String sourceDirectory) {
            this.sourceCode = sourceCode;
            this.sourceDirectory = sourceDirectory;
        }

        String getSourceCode() {
            return sourceCode;
        }

        String getSourceDirectory() {
            return sourceDirectory;
        }
    }

    /***
     * @param typeElement A type with {@link Modifier#PUBLIC} that is located in the project being annotation processed.
     * @return the source code associated with {@code typeElement}.
     */
    private SourceInformation getSourceInformation(final TypeElement typeElement) {
        final String annotatedClassPackageName = AnnotationPreProcessor.getPackageName(typeElement).toString();
        final String annotatedClassSimpleName = typeElement.getSimpleName().toString();

        final String annotatedClassSourceFileName = annotatedClassSimpleName + ".java";

        try {
            final FileObject fileObject = getFiler().getResource(
                StandardLocation.SOURCE_PATH,
                annotatedClassPackageName,
                annotatedClassSourceFileName
            );

            final String sourceCode = fileObject.getCharContent(true).toString();

            final URI fileObjectURI = fileObject.toUri();
            final Path fileObjectPath = Paths.get(fileObjectURI);
            final String fileObjectPathString = fileObjectPath.toString();

            final String sourceDirectory;

            if (annotatedClassPackageName.isEmpty()) {
                final int classDirectoryAndClassSeparatorIndex = fileObjectPathString.lastIndexOf(File.separator);

                sourceDirectory = fileObjectPathString.substring(0, classDirectoryAndClassSeparatorIndex);
            }
            else {
                final String packageAsDirectory = annotatedClassPackageName.replace(".", File.separator);
                final int packageIndexInAbsoluteDirectory = fileObjectPathString.lastIndexOf(packageAsDirectory);

                sourceDirectory = fileObjectPathString.substring(0, packageIndexInAbsoluteDirectory - 1);
            }

            return new SourceInformation(sourceCode, sourceDirectory);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    SpecializationWriter(
        final Filer filer,
        final TypeElement annotatedTypeElement,
        final ClassToTypeMirror classToTypeMirror
    ) {
        this.filer = filer;
        this.annotatedTypeElement = annotatedTypeElement;
        this.annotatedClassSourceInformation = getSourceInformation(annotatedTypeElement);
        this.classToTypeMirror = classToTypeMirror;

        this.typeSolver = newTypeSolver();
    }

    private static int getSpecializedPrimitivesCount(final PrimitiveSpecialization primitiveSpecialization) {
        int specializedPrimitivesCount = 0;

        if (primitiveSpecialization.forGeneric()) specializedPrimitivesCount++;
        if (primitiveSpecialization.forByte()) specializedPrimitivesCount++;
        if (primitiveSpecialization.forChar()) specializedPrimitivesCount++;
        if (primitiveSpecialization.forShort()) specializedPrimitivesCount++;
        if (primitiveSpecialization.forInt()) specializedPrimitivesCount++;
        if (primitiveSpecialization.forLong()) specializedPrimitivesCount++;
        if (primitiveSpecialization.forFloat()) specializedPrimitivesCount++;
        if (primitiveSpecialization.forDouble()) specializedPrimitivesCount++;

        return specializedPrimitivesCount;
    }


    private static PrimitiveType[] getSpecializedPrimitives(final PrimitiveSpecialization primitiveSpecialization) {
        if (primitiveSpecialization == null) {
            return null;
        }

        final int specializedPrimitivesCount = getSpecializedPrimitivesCount(primitiveSpecialization);

        final PrimitiveType[] primitiveTypes = new PrimitiveType[specializedPrimitivesCount];

        int specializedPrimitiveIndex = 0;

        if (primitiveSpecialization.forGeneric()) primitiveTypes[specializedPrimitiveIndex++] = null;
        if (primitiveSpecialization.forByte()) primitiveTypes[specializedPrimitiveIndex++] = PrimitiveType.byteType();
        if (primitiveSpecialization.forChar()) primitiveTypes[specializedPrimitiveIndex++] = PrimitiveType.charType();
        if (primitiveSpecialization.forShort()) primitiveTypes[specializedPrimitiveIndex++] = PrimitiveType.shortType();
        if (primitiveSpecialization.forInt()) primitiveTypes[specializedPrimitiveIndex++] = PrimitiveType.intType();
        if (primitiveSpecialization.forLong()) primitiveTypes[specializedPrimitiveIndex++] = PrimitiveType.longType();
        if (primitiveSpecialization.forFloat()) primitiveTypes[specializedPrimitiveIndex++] = PrimitiveType.floatType();
        if (primitiveSpecialization.forDouble()) primitiveTypes[specializedPrimitiveIndex] = PrimitiveType.doubleType();

        return primitiveTypes;
    }

    private static final PrimitiveType[] DEFAULT_PRIMITIVE_TYPE_COLUMN_CHOICES =  {
        PrimitiveType.intType(),
        PrimitiveType.longType(),
        PrimitiveType.doubleType()
    };

    private PrimitiveType[][] getPrimitiveTypesCombinations(final PrimitiveSpecialization[] primitiveSpecializationArray,
                                                            final int typeParameterCount) {
        final PrimitiveType[][] twoDimPrimitiveTypes;

        if (primitiveSpecializationArray.length == 0) {
            twoDimPrimitiveTypes = new PrimitiveType[typeParameterCount][];
            Arrays.fill(twoDimPrimitiveTypes, DEFAULT_PRIMITIVE_TYPE_COLUMN_CHOICES);
        }
        else {
            twoDimPrimitiveTypes = Arrays.stream(primitiveSpecializationArray).map(SpecializationWriter::getSpecializedPrimitives)
                                                                              .toArray(PrimitiveType[][]::new);
        }

        return twoDimPrimitiveTypes;
    }

    void generateSpecializationPerCombination(final PrimitiveSpecializations primitiveSpecializations, final int typeParameterCount) {
        final PrimitiveSpecialization[] primitiveSpecializationArray = primitiveSpecializations.value();

        final PrimitiveType[][] twoDimPrimitiveTypes = getPrimitiveTypesCombinations(
            primitiveSpecializationArray,
            typeParameterCount
        );

        generateSpecializationPerCombination(twoDimPrimitiveTypes, primitiveSpecializations);
    }

    private TypeSolver newTypeSolver() {
        final String sourceDirectory = getAnnotatedClassSourceInformation().getSourceDirectory();

        final TypeSolver standardLibraryTypeSolver = new ReflectionTypeSolver();

        final JavaParserTypeSolver processedProjectTypeSolver = new JavaParserTypeSolver(sourceDirectory);

        final CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver(
            standardLibraryTypeSolver,
            processedProjectTypeSolver
        );

        final ClassLoader processedProjectClassLoader = getClass().getClassLoader();
        final URLClassLoader processedProjectURLClassLoader = (URLClassLoader) processedProjectClassLoader;

        final URL[] dependencyLocationURLs = processedProjectURLClassLoader.getURLs();

        try {
            for (final URL dependencyLocationURL : dependencyLocationURLs) {
                final URI dependencyLocationURI = dependencyLocationURL.toURI();
                final Path dependencyLocationPath = Paths.get(dependencyLocationURI);

                if (dependencyLocationPath.toString().endsWith(".jar")) {
                    final TypeSolver dependencyTypeSolver = new JarTypeSolver(dependencyLocationPath);

                    combinedTypeSolver.add(dependencyTypeSolver);
                }
            }
        }
        catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
        catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return combinedTypeSolver;
    }

    private void generateSpecializationPerCombination(final PrimitiveType[][] twoDimPrimitiveTypes, final PrimitiveSpecializations primitiveSpecializations) {
        final ReplacementConfiguration replacementConfiguration = primitiveSpecializations.replacementConfiguration();
        final FormattedClassSpecifier primitiveSpecializationClassSpecifier = primitiveSpecializations.formattedClassSpecifier();

        // keep track of the size of each inner String array
        final int[] sizeArray = new int[twoDimPrimitiveTypes.length];

        // keep track of the index of each inner String array which will be used
        // to make the next combination
        final int[] counterArray = new int[twoDimPrimitiveTypes.length];

        // Discover the size of each inner array and populate sizeArray.
        // Also calculate the total number of combinations possible using the
        // inner String array sizes.
        int totalCombinationCount = 1;

        for (int columnIndex = 0; columnIndex < twoDimPrimitiveTypes.length; columnIndex++) {
            final PrimitiveType[] columnChoices = twoDimPrimitiveTypes[columnIndex];

            sizeArray[columnIndex] = columnChoices.length;
            totalCombinationCount *= columnChoices.length;
        }

        final PrimitiveType[] currentChoicesPermutation = new PrimitiveType[twoDimPrimitiveTypes.length];

        final String annotatedClassSourceCode = getAnnotatedClassSourceInformation().getSourceCode();

        for (int countdown = totalCombinationCount; countdown > 0; countdown--) {
            // Run through the inner arrays, grabbing the member from the index
            // specified by the counterArray for each inner array, and build a
            // combination string.
            for(int columnIndex = 0; columnIndex < twoDimPrimitiveTypes.length; ++columnIndex) {
                final PrimitiveType[] possibleColumnChoices = twoDimPrimitiveTypes[columnIndex];
                final int nextColumnChoiceIndex = counterArray[columnIndex];

                currentChoicesPermutation[columnIndex] = possibleColumnChoices[nextColumnChoiceIndex];
            }

            final CompilationUnit specializedClassCompilationUnit = JavaParser.parse(annotatedClassSourceCode);

            final String annotatedTypeElementSimpleName = getAnnotatedTypeElement().getSimpleName().toString();

            final SpecializationVisitor specializationVisitor = new SpecializationVisitor(
                specializedClassCompilationUnit,
                annotatedTypeElementSimpleName,
                replacementConfiguration,
                getTypeSolver(),
                getClassToTypeMirror()
            );

            final PrimitiveTypesCombination primitiveTypesCombination = new PrimitiveTypesCombination(
                currentChoicesPermutation,
                annotatedTypeElementSimpleName,
                primitiveSpecializationClassSpecifier
            );

            specializationVisitor.visitOrderly(primitiveTypesCombination);

            final String annotatedClassPackageName = AnnotationPreProcessor.getPackageName(getAnnotatedTypeElement()).toString();
            final String annotatedClassName = getAnnotatedTypeElement().getSimpleName().toString();

            final String preProcessedFullyQualifiedName = AnnotationPreProcessor.getPreProcessedFullyQualifiedName(
                primitiveSpecializations.formattedClassSpecifier(),
                annotatedClassPackageName,
                annotatedClassName,
                currentChoicesPermutation
            );

            generateFile(preProcessedFullyQualifiedName, specializedClassCompilationUnit);

            // Now we need to increment the counterArray so that the next
            // combination is taken on the next iteration of this loop.
            for(int incIndex = twoDimPrimitiveTypes.length - 1; incIndex >= 0; --incIndex) {
                if(counterArray[incIndex] + 1 < sizeArray[incIndex]) {
                    ++counterArray[incIndex];
                    // None of the indices of higher significance need to be
                    // incremented, so jump out of this for loop at this point.
                    break;
                }
                // The index at this position is at its max value, so zero it
                // and continue this loop to increment the index which is more
                // significant than this one.
                counterArray[incIndex] = 0;
            }
        }
    }



    private void generateFile(final String newFullyQualifiedName,
                              final CompilationUnit newClassCompilationUnit) {
        try {
            final String generatedClassSource = newClassCompilationUnit.toString();

            final JavaFileObject generatedFileObject = getFiler().createSourceFile(newFullyQualifiedName);

            try (final Writer generatedClassWriter = generatedFileObject.openWriter()) {
                generatedClassWriter.write(generatedClassSource);
            }
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
    }
}
