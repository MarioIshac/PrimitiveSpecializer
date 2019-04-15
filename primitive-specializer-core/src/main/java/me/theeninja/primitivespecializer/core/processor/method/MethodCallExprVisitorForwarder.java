package me.theeninja.primitivespecializer.core.processor.method;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.types.ResolvedType;
import lombok.AccessLevel;
import lombok.Getter;
import me.theeninja.primitivespecializer.core.annotation.SynchronizationReplacementType;
import me.theeninja.primitivespecializer.core.processor.PrimitiveTypesCombination;

@Getter(AccessLevel.PRIVATE)
public class MethodCallExprVisitorForwarder {
    private final InstanceInitializingMethodCallExprVisitor instanceInitializingMethodCallExprVisitor;
    private final StaticInitializingMethodCallExprVisitor staticInitializingMethodCallExprVisitor;

    public MethodCallExprVisitorForwarder(final SynchronizationReplacementType synchronizationReplacementType) {
        this.instanceInitializingMethodCallExprVisitor = new InstanceInitializingMethodCallExprVisitor(synchronizationReplacementType);
        this.staticInitializingMethodCallExprVisitor = new StaticInitializingMethodCallExprVisitor();
    }

    public Visitable visitInstanceInitializingMethodCallExpr(final MethodCallExpr methodCallExpr, final ResolvedType resolvedType, final PrimitiveTypesCombination primitiveTypesCombination) {
        return getInstanceInitializingMethodCallExprVisitor().visitMethodCallExpr(methodCallExpr, resolvedType, primitiveTypesCombination);
    }

    public Visitable visitStaticInitializingMethodCallExpr(final MethodCallExpr methodCallExpr, final ResolvedType resolvedType, final PrimitiveTypesCombination primitiveTypesCombination) {
        return getStaticInitializingMethodCallExprVisitor().visitMethodCallExpr(methodCallExpr, resolvedType, primitiveTypesCombination);
    }
}
