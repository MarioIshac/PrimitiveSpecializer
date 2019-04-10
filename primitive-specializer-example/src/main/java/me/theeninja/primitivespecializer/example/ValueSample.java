package me.theeninja.primitivespecializer.example;

import me.theeninja.primitivespecializer.core.annotation.ReplacementConfiguration;
import org.checkerframework.common.value.qual.IntVal;

public class ValueSample {
    private static class Value<V> {
        private Value(V value) {
            this.value = value;
        }

        private final V value;

        public V getValue() {
            return value;
        }
    }

    private static class ValueConstructor {
        public static <V> Value<V> create(V value) {
            return new Value<>(value);
        }
    }

    private static class IntValue {
        private IntValue(int value) {
            this.value = value;
        }

        private final int value;

        public int getValue() {
            return value;
        }
    }

    private static class IntValueConstructor {
        public static IntValue create(int value) {
            return new IntValue(value);
        }
    }

    private static class ValueConstructorInvoker {
        Value<String> stringValue = ValueConstructor.<String>create("_");
        IntValue intValue = IntValueConstructor.create(5);


    }
}
