package me.theeninja.primitivespecializer.core.processor.method;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedType;
import lombok.Getter;
import me.theeninja.primitivespecializer.core.annotation.SynchronizationReplacementType;
import me.theeninja.primitivespecializer.core.processor.PrimitiveTypesCombination;

@Getter
public class InstanceInitializingMethodCallExprVisitor implements InitializingMethodCallExprVisitor {
    private final SynchronizationReplacementType synchronizationReplacementType;
    private boolean isLockRequired;

    public static final String LOCK_NAME = "lock";

    public InstanceInitializingMethodCallExprVisitor(final SynchronizationReplacementType synchronizationReplacementType) {
        this.synchronizationReplacementType = synchronizationReplacementType;
    }

    @Override
    public Visitable visitMethodCallExpr(final MethodCallExpr methodCallExpr, final ResolvedType resolvedType, final PrimitiveTypesCombination primitiveTypesCombination) {
        // The types of variable have been updated before the method call expressions are visited
        if (!resolvedType.isPrimitive()) {
            return methodCallExpr;
        }

        final ResolvedPrimitiveType resolvedPrimitiveType = resolvedType.asPrimitive();

        final NameExpr callInitiator = getGuaranteedMethodCallScope(methodCallExpr);

        final String methodName = methodCallExpr.getNameAsString();

        switch (methodName) {
            case "equals": {
                final Expression callArgument = methodCallExpr.getArguments().get(0);

                BinaryExpr equivalentBinaryExpr = newBinaryExpr(callInitiator, callArgument, BinaryExpr.Operator.EQUALS);

                methodCallExpr.replace(equivalentBinaryExpr);

                return methodCallExpr;
            }
            case "compareTo": {
                final Expression callArgument = methodCallExpr.getArguments().get(0);

                BinaryExpr equivalentBinaryExpr = newBinaryExpr(callInitiator, callArgument, BinaryExpr.Operator.LESS_EQUALS);

                methodCallExpr.replace(equivalentBinaryExpr);

                return methodCallExpr;
            }
            case "getClass": {
                final String classLiteralName = resolvedPrimitiveType.getBoxTypeQName();

                /* final ClassExpr primitiveClassExpression = new ClassExpr(classLiteralName);

                methodCallExpr.replace(primitiveClassExpression); */

                return methodCallExpr;

            }
            case "intValue": {
                return new CastExpr(PrimitiveType.intType(), callInitiator);
            }
            case "byteValue": {
                return new CastExpr(PrimitiveType.byteType(), callInitiator);
            }
            case "shortValue": {
                return new CastExpr(PrimitiveType.shortType(), callInitiator);
            }
            case "longValue": {
                return new CastExpr(PrimitiveType.longType(), callInitiator);
            }
            case "floatValue": {
                return new CastExpr(PrimitiveType.floatType(), callInitiator);
            }
            case "doubleValue": {
                return new CastExpr(PrimitiveType.doubleType(), callInitiator);
            }

            // TODO Throw a compile-time error if a variable with a name equivalent to any boxed primitive type is detected
            case "hashCode": {
                final String primitiveBoxedTypeName = resolvedPrimitiveType.getBoxTypeQName();
                final NameExpr primitiveBoxedTypeAsCaller = new NameExpr(primitiveBoxedTypeName);

                methodCallExpr.setScope(primitiveBoxedTypeAsCaller);
                methodCallExpr.getArguments().add(callInitiator);

                return methodCallExpr;
            }
            case "wait":
            case "notify":
            case "notifyAll": {
                switch (getSynchronizationReplacementType()) {
                    case FORBID: {
                        throw new IllegalArgumentException();
                    }
                    case APPEND_PARAMETER: {
                        this.isLockRequired = true;

                        final NameExpr nameExpr = new NameExpr(LOCK_NAME);
                        methodCallExpr.setScope(nameExpr);

                        return methodCallExpr;
                    }
                }
            }
            default: {
                return methodCallExpr;
            }
        }
    }

    private static BinaryExpr newBinaryExpr(final Expression callInitiator, final Expression callArgument, final BinaryExpr.Operator operator) {
        final BinaryExpr binaryExpr = new BinaryExpr();
        binaryExpr.setLeft(callInitiator);
        binaryExpr.setRight(callArgument);
        binaryExpr.setOperator(operator);

        return binaryExpr;
    }
}
