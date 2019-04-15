package me.theeninja.primitivespecializer.core.processor.method;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.types.ResolvedType;
import javassist.expr.MethodCall;
import me.theeninja.primitivespecializer.core.processor.PrimitiveTypesCombination;

import java.util.Optional;

public interface InitializingMethodCallExprVisitor {
    Visitable visitMethodCallExpr(final MethodCallExpr methodCallExpr, final ResolvedType methodCaller, final PrimitiveTypesCombination primitiveTypesCombination);

    default NameExpr getGuaranteedMethodCallScope(final MethodCallExpr methodCallExpr) {
        Optional<Expression> guaranteedMethodScope = methodCallExpr.getScope();

        return guaranteedMethodScope.filter(Expression::isNameExpr)
                                    .map(Expression::asNameExpr)
                                    .orElseThrow(AssertionError::new);
    }
}
