package me.theeninja.primitivespecializer.core.annotation;

import java.lang.annotation.*;

/**
 * Specifies that the annotated class should have its type parameters specialized with primitive types
 * dependent on {@link PrimitiveSpecializations#value()}. There will be N permutations of X primitive types each, where
 * X is the number of type arguments being specialized by primitive unwrapped types. These N permutations will lead to
 * N generated classes that have the same source as the annotated class except for the following changes.
 *
 * 1) All uses of the type argument are replaced with the primitive type part of the permutation used to generate the class,
 * corresponding to said type argument. For example, given the type arguments {@code [A, B, C]} and a primitive types permutation
 * {@code [int, long, double]}, {@code A} would be replaced with {@code int}, {@code B} with {@code long}, and {@code C} with {@code double}.
 *
 * 2) Procedures that rely on the use of reference types (subclasses of {@link Object}) as opposed to value types (the primitive types)
 * will be changed based on the replacement strategies specified through {@link PrimitiveSpecializations#replacementConfiguration()}.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface PrimitiveSpecializations {
    /**
     *
     * @see FormattedClassSpecifier
     */
    FormattedClassSpecifier formattedClassSpecifier() default @FormattedClassSpecifier;

    /**
     *
     *
     * @see PrimitiveSpecialization
     */
    PrimitiveSpecialization[] value() default {};

    /**
     *
     * @see ReplacementConfiguration
     */
    // Default (empty) array is equivalent to array populated with default `me.theeninja.me.theeninja.primitivespecializer.core.annotation.PrimitiveSpecialization` values
    ReplacementConfiguration replacementConfiguration() default @ReplacementConfiguration;
}
