package me.theeninja.primitivespecializer.example;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import me.theeninja.primitivespecializer.core.annotation.*;

import java.lang.reflect.Array;
import java.util.stream.Stream;

import static java.util.stream.Stream.*;

@PrimitiveSpecializations(
    formattedClassSpecifier = @FormattedClassSpecifier(
        classNameComponents = {"<0>", "To<1>", "Map"}
    ),

    replacementConfiguration = @ReplacementConfiguration(
        classReplacements = @ClassReplacement(
            oldClass = Stream.class,

            formattedClassSpecifier = @FormattedClassSpecifier(
                classNameComponents = {"<0>", "Stream"}
            )
        ),

        synchronizationReplacementType = SynchronizationReplacementType.APPEND_PARAMETER,

        nullReplacement = @NullReplacement(
            nullReplacementType = NullReplacementType.RUN_TIME_EXCEPTION,

            nullReplacementException = @NullReplacementException(
                exception = IllegalStateException.class,
                exceptionMessage = "Caller expected key to be in map, illegal state on caller's part."
            )
        )
    )
)
public class Map<K, V> {
    @SuppressWarnings("unchecked")
    public Map(final int capacity) {
        this.hashes = (Node[]) new Object[capacity];
    }

    private final Node[] hashes;

    private int getPartition(final K key) {
        return key.hashCode() / getHashes().length;
    }

    public void remove(final K key) {
        final int partitionOfKey = getPartition(key);

        Node previousNode = null;
        Node currentNode = getHashes()[partitionOfKey];

        while (currentNode != null) {
            if (currentNode.getKey() == key) {
                if (previousNode == null) {
                    getHashes()[partitionOfKey] = currentNode.getNext();
                }

                else {
                    final Node previousNodeNewNext = currentNode.getNext();

                    previousNode.setNext(previousNodeNewNext);
                    return;
                }
            }

            else {
                previousNode = currentNode;
                currentNode = currentNode.getNext();
            }
        }
    }

    public V get(final K key) {
        final int partitionOfKey = getPartition(key);

        Node node = getHashes()[partitionOfKey];

        while (node != null) {
            if (node.getKey() == key) {
                return node.getValue();
            }

            node = node.getNext();
        }

        return null;
    }

    public void put(final K key, final V value) {
        final int partitionOfKey = getPartition(key);

        Node addedNode = new Node();
        addedNode.setKey(key);
        addedNode.setValue(value);

        Node node = getHashes()[partitionOfKey];

        if (node == null) {
            getHashes()[partitionOfKey] = addedNode;
        }
        else {
            while (node.getNext() != null) {
                node = node.getNext();
            }

            node.setNext(addedNode);
        }
    }

    public Stream<K> streamKeys() {
        final Builder<K> streamBuilder = Stream.<K>builder();

        for (Node node : getHashes()) {
            while (node != null) {
                final K nodeAttribute = node.getKey();
                streamBuilder.accept(nodeAttribute);
                node = node.getNext();
            }
        }

        return streamBuilder.build();
    }

    public Stream<V> streamValues() {
        final Builder<V> streamBuilder = Stream.<V>builder();

        for (Node node : getHashes()) {
            while (node != null) {
                final V nodeAttribute = node.getValue();
                streamBuilder.accept(nodeAttribute);
                node = node.getNext();
            }
        }

        return streamBuilder.build();
    }

    private Node[] getHashes() {
        return hashes;
    }

    private class Node {
        private K key;
        private V value;

        private Node next;

        K getKey() {
            return key;
        }

        void setKey(K key) {
            this.key = key;
        }

        V getValue() {
            return value;
        }

        void setValue(V value) {
            this.value = value;
        }

        Node getNext() {
            return next;
        }

        void setNext(Node next) {
            this.next = next;
        }
    }
}
