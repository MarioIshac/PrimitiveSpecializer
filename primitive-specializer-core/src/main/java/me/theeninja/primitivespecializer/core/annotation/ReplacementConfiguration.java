package me.theeninja.primitivespecializer.core.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies strategies for replacing X with Y. Y may be one of the following:
 *
 * 1) An optimal version of X that is designed specifically for primitive-specialization classes. For example,
 * {@code IntStream} instead of {@code Stream<Integer>}. These replacements do not cause compiler errors if not done.
 * 2) A required version of X that is designed specifically for primitive-specialization classes. For example,
 * {@code return 0;} instead of {@code return null}.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface ReplacementConfiguration {
    /**
     * Represents replacements of type 1 in the documentation of {@link ReplacementConfiguration}. Old classes will
     * be replaced with a new class. This new class is determined by inserting the string representation of the primitive type
     * into the name of the new class, which has placeholders for the primitive type. Generic parameters are automatically
     * handled.
     *
     * Default: No old classes to replace.
     */
    ClassReplacement[] classReplacements() default {};

    /**
     *
     * @return
     */
    NullReplacement nullReplacement() default @NullReplacement;

    /**
     *
     * @return
     */
    SynchronizationReplacementType synchronizationReplacementType() default SynchronizationReplacementType.FORBID;
}
