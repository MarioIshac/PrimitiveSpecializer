package me.theeninja.primitivespecializer.core.annotation;

import me.theeninja.primitivespecializer.core.annotation.classreplacement.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a mapping between an old, known class and a list of replacement classes. These replacement classes are all
 * collectively specified through {@link FormattedClassSpecifier}, as various primitive types are placed into the
 * {@link FormattedClassSpecifier#classNameComponents()}
 *
 * This mapping is used to change generic components of generated primitive-specializations into primitive-spcialized components.
 *
 * A {@link ClassReplacement} that represents the mapping
 *
 * {@code
 *      oldClass = Stream.class,
 *      formattedClassSpecifier = @FormattedClassSpecifier(className = {0}Stream)
 * }
 *
 * would replace every occurrence of {@code Stream<WrappedPrimitiveType>} with
 * {@code [unwrappedPrimitiveType]Stream}, e.g {@code Stream<Integer>} to {@code IntStream}
 */
@Retention(RetentionPolicy.CLASS)
@Target({})
public @interface ClassReplacement {
    /**
     * Generic class to replace with primitive-specialized classes.
     */
    Class<?> oldClass();

    /**
     * Formatted class specifier that indirectly specifies which primitive specializations will replace {@code oldClass}.
     */
    FormattedClassSpecifier formattedClassSpecifier() default @FormattedClassSpecifier;

    /**
     * Determines whether the mapping of generic {@code oldClass} -> primitive-specialized {@code oldClass}
     * should apply to its subclasses. The {@link ClassReplacement} example in this class' documentation, given that
     * this attribute is true (by default), would also map {@code Stream.Builder<Integer>} to
     * {@code IntStream.Builder}. This mapping of subclasses is not generated if there is no subclass of an equivalent name
     * in the formatted primitive-specialization name (in this case, that is {@code IntStream}).
     */
    boolean inherited() default ClassReplacementDefault.INHERITED_DEFAULT_VALUE;
}
