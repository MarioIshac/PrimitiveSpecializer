package me.theeninja.primitivespecializer.core.annotation.classreplacement;

import java.util.Spliterators;
import java.util.function.Supplier;

public class SupplierClassReplacement extends ClassReplacementDefault {
    @Override
    public Class<?> oldClass() {
        return Supplier.class;
    }
}
