package me.theeninja.primitivespecializer.core.processor.method;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import me.theeninja.primitivespecializer.core.processor.PrimitiveTypesCombination;

import java.util.Arrays;

public class StaticInitializingMethodCallExprVisitor implements InitializingMethodCallExprVisitor {
    @Override
    public Visitable visitMethodCallExpr(final MethodCallExpr methodCallExpr, final ResolvedType methodCaller, final PrimitiveTypesCombination primitiveTypesCombination) {
        /* ResolvedReferenceType resolvedReferenceType = methodCaller.asReferenceType();

        final String qualifiedName = resolvedReferenceType.getQualifiedName();

        switch (qualifiedName) {
            case "java.lang.reflect.Arrays": {
                final String methodName = methodCallExpr.getNameAsString();

                if (methodName.equals("newInstance")) {

                }
            }
        }

        final String methodName = methodCallExpr.getNameAsString(); */

        return methodCallExpr;
    }
}
