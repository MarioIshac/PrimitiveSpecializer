package me.theeninja.primitivespecializer.core.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface NullReplacementException {
    Class<? extends RuntimeException> exception() default IllegalArgumentException.class;
    String exceptionMessage() default "Illegal argument provided, such that no result was found";
}
