package me.theeninja.primitivespecializer.core.annotation;

/**
 * Indicates what should be done in the event that a `return null` is stumbled upon in a method which originally
 * returned a variable of y
 */
public enum NullReplacementType {
    VALUE_REPLACEMENT,
    COMPILE_TIME_EXCEPTION,
    RUN_TIME_EXCEPTION;
}
