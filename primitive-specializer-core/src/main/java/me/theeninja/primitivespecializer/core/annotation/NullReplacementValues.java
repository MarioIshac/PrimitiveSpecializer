package me.theeninja.primitivespecializer.core.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface NullReplacementValues {
    boolean forBoolean() default false;
    char forChar() default 0;
    byte forByte() default 0;
    short forShort() default 0;
    int forInt() default 0;
    long forLong() default 0;
    float forFloat() default 0;
    double forDouble() default 0;
}
