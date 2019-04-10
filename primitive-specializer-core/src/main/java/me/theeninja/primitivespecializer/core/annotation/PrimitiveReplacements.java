package me.theeninja.primitivespecializer.core.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static me.theeninja.primitivespecializer.core.annotation.classreplacement.PrimitiveReplacementsDefault.*;

@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface PrimitiveReplacements {
    String forBoolean() default BOOLEAN_REPLACEMENT;
    String forByte() default BYTE_REPLACEMENT;
    String forChar() default CHAR_REPLACEMENT;
    String forShort() default SHORT_REPLACEMENT;
    String forInt() default INT_REPLACEMENT;
    String forLong() default LONG_REPLACEMENT;
    String forFloat() default FLOAT_REPLACEMENT;
    String forDouble() default DOUBLE_REPLACEMENT;
}
