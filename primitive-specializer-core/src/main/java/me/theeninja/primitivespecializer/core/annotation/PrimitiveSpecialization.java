package me.theeninja.primitivespecializer.core.annotation;

import com.github.javaparser.ast.type.PrimitiveType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface PrimitiveSpecialization {
    /**
     * Determines whether the parameter type represented by this {@link PrimitiveSpecialization} should be specialized
     * for ANY primitive. Note the word ANY, this means that a {@code false} here overrides any {@code true} for
     * any other value of PrimitiveSpecialization.
     */
    boolean forGeneric() default false;

    /**
     * Determines whether the parameter type represented by this {@link PrimitiveSpecialization} should be specialized
     * for booleans.
     */
    boolean forBoolean() default false;

    /**
     * Determines whether the parameter type represented by this {@link PrimitiveSpecialization} should be specialized
     * for bytes.
     */
    boolean forByte() default false;

    /**
     * Determines whether the parameter type represented by this {@link PrimitiveSpecialization} should be specialized
     * for characters.
     */
    boolean forChar() default false;

    /**
     * Determines whether the parameter type represented by this {@link PrimitiveSpecialization} should be specialized
     * for shorts.
     */
    boolean forShort() default false;

    /**
     * Determines whether the parameter type represented by this {@link PrimitiveSpecialization} should be specialized
     * for ints.
     */
    boolean forInt() default true;

    /**
     * Determines whether the parameter type represented by this {@link PrimitiveSpecialization} should be specialized
     * for longs.
     */
    boolean forLong() default true;

    /**
     * Determines whether the parameter type represented by this {@link PrimitiveSpecialization} should be specialized
     * for floats.
     */
    boolean forFloat() default false;

    /**
     * Determines whether the parameter type represented by this {@link PrimitiveSpecialization} should be specialized
     * for doubles.
     */
    boolean forDouble() default true;
}
