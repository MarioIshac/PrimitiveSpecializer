package me.theeninja.primitivespecializer.core.annotation.classreplacement;

import me.theeninja.primitivespecializer.core.annotation.PrimitiveReplacements;

import java.lang.annotation.Annotation;

public class PrimitiveReplacementsDefault implements PrimitiveReplacements {
    public static final String BOOLEAN_REPLACEMENT = "Boolean";
    public static final String BYTE_REPLACEMENT = "Byte";
    public static final String CHAR_REPLACEMENT = "Char";
    public static final String SHORT_REPLACEMENT = "Short";
    public static final String INT_REPLACEMENT = "Int";
    public static final String LONG_REPLACEMENT = "Long";
    public static final String FLOAT_REPLACEMENT = "Float";
    public static final String DOUBLE_REPLACEMENT = "Double";

    @Override
    public String forBoolean() {
        return BOOLEAN_REPLACEMENT;
    }

    @Override
    public String forByte() {
        return BYTE_REPLACEMENT;
    }

    @Override
    public String forChar() {
        return CHAR_REPLACEMENT;
    }

    @Override
    public String forShort() {
        return SHORT_REPLACEMENT;
    }

    @Override
    public String forInt() {
        return INT_REPLACEMENT;
    }

    @Override
    public String forLong() {
        return LONG_REPLACEMENT;
    }

    @Override
    public String forFloat() {
        return FLOAT_REPLACEMENT;
    }

    @Override
    public String forDouble() {
        return DOUBLE_REPLACEMENT;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return PrimitiveReplacements.class;
    }
}
