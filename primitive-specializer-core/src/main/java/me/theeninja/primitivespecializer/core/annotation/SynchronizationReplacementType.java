package me.theeninja.primitivespecializer.core.annotation;

/**
 * Determines the replacement strategy when, assuming {@code genericVariable} is a variable with a generic type that
 * is targeted by a {@link PrimitiveSpecialization}, the following code is stumbled upon in the class annotated with
 * {@link PrimitiveSpecializations}.
 *
 * <br>
 *
 * <code>
 *      synchronized(genericVariable) {
 *          ...
 *      }
 * </code>
 *
 * Since {@code genericVariable}, after specialization, will be of a primitive type, it cannot be used as a lock.
 */
public enum SynchronizationReplacementType {
    /**
     * Throw an error if generic variables of a generic type that is targeted by a {@link PrimitiveSpecialization}
     * are serving as locks of synchronized statements. Processing stops.
     */
    FORBID,

    /**
     * Appends X parameters that represent locks of type {@link Object} to the constructor, where X is the number
     * of generic types which have atleast one associated generic variable (of that type) that is the target of
     * synchronized statement(s). Processing continues with this modification.
     */
    APPEND_PARAMETER;
}
