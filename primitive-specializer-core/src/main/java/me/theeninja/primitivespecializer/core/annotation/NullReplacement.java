package me.theeninja.primitivespecializer.core.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Determines the replacement strategy when, assuming {@code genericVariable} is a variable with a generic type ({@Code genericType})
 * that is targeted by a {@link PrimitiveSpecialization}, the following code is stumbled upon in a method with return type
 * {@code genericType} in a class annotated with {@link PrimitiveSpecializations}.
 * <br>
 *
 * <code>
 *      return null;
 * </code>
 *
 * Since {@code genericVariable}, after specialization, will be of a primitive type (and thus not a reference),
 * it cannot be used as a null reference.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface NullReplacement {
    NullReplacementType nullReplacementType() default NullReplacementType.COMPILE_TIME_EXCEPTION;

    NullReplacementValues nullReplacementValues() default @NullReplacementValues;
    NullReplacementException nullReplacementException() default @NullReplacementException;
}
