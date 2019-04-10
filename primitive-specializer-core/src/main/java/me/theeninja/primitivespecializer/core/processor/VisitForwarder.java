package me.theeninja.primitivespecializer.core.processor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.function.BiConsumer;

@AllArgsConstructor
@Getter
class VisitForwarder {
    private final CompilationUnit compilationUnit;
    private final PrimitiveTypesCombination primitiveTypesCombination;

    <T extends Node> void visit(final Class<T> nodeClass, final BiConsumer<T, ? super PrimitiveTypesCombination> nodeVisitor) {
        final List<T> targetNodes = getCompilationUnit().findAll(nodeClass);

        for (final T targetNode : targetNodes) {
            nodeVisitor.accept(targetNode, getPrimitiveTypesCombination());
        }
    }
}
