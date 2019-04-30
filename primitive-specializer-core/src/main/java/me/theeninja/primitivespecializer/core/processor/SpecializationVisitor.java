package me.theeninja.primitivespecializer.core.processor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.java.Log;
import me.theeninja.primitivespecializer.core.annotation.*;
import me.theeninja.primitivespecializer.core.processor.method.MethodCallExprVisitorForwarder;

import java.util.Optional;
import java.util.stream.Stream;

@Log
@Getter(AccessLevel.PRIVATE)
public class SpecializationVisitor extends ModifierVisitor<PrimitiveTypesCombination> {
    private final CompilationUnit compilationUnit;
    private final TypeSpecializationForwarder typeSpecializationForwarder;
    private final MethodCallExprVisitorForwarder methodCallExprVisitorForwarder;

    SpecializationVisitor(
        final CompilationUnit compilationUnit,
        final String fullyQualifiedAnnotatedClassName,
        final ReplacementConfiguration replacementConfiguration,
        final TypeSolver typeSolver,
        final ClassToTypeMirror classToTypeMirror
    ) {
        this.fullyAnnotatedClassName = fullyQualifiedAnnotatedClassName;

        Optional<ClassOrInterfaceDeclaration> optionalDeclaredAnnotatedClass = compilationUnit.getClassByName(getFullyAnnotatedClassName());

        if (optionalDeclaredAnnotatedClass.isPresent()) {
            final ClassOrInterfaceDeclaration declaredAnnotationClass = optionalDeclaredAnnotatedClass.get();

            this.annotatedElementTypeParameters = declaredAnnotationClass.getTypeParameters();
        }
        else {
            throw new AssertionError(
                "`SpecializationVisitor` should have been internally constructed with a found declared" +
                " annotation class, instead given " + "\"" + getFullyAnnotatedClassName() + "\""
            );
        }

        this.compilationUnit = compilationUnit;
        this.replacementConfiguration = replacementConfiguration;

        this.methodCallExprVisitorForwarder = new MethodCallExprVisitorForwarder(getReplacementConfiguration().synchronizationReplacementType());

        this.javaParserFacade = JavaParserFacade.get(typeSolver);
        this.classToTypeMirror = classToTypeMirror;

        this.typeSpecializationForwarder = new TypeSpecializationForwarder(this);
    }

    void visitOrderly(final PrimitiveTypesCombination primitiveTypesCombination) {
        VisitForwarder visitForwarder = new VisitForwarder(getCompilationUnit(), primitiveTypesCombination);

        log.info("");
        // First change variable types / ways of creating variables
        visitForwarder.visit(VariableDeclarator.class, this::visit);

        // Change method parameter types / method return types
        visitForwarder.visit(MethodDeclaration.class, this::visit);

        // Then change method calls with these variables, noting that there types are now primitive
        // This also observes if any variables are used as locks
        visitForwarder.visit(MethodCallExpr.class, this::visit);

        // Then check if variable is used as lock within synchronization statement
        visitForwarder.visit(SynchronizedStmt.class, this::visit);

        /* If a variable was detected to be used as lock, either through a locking method or a synchronziation statement,
        then modify the constructor if appropiate */
        visitForwarder.visit(ConstructorDeclaration.class, this::visit);

        /* Finally, change the declaration of the class that is being generated, including changing the name,
        specializing the parameters, etc. */
        visitForwarder.visit(ClassOrInterfaceDeclaration.class, this::visit);
    }

    private LiteralExpr newReplacingLiteral(final PrimitiveType.Primitive primitive, final NullReplacementValues nullReplacementValues) {
        switch (primitive) {
            case BOOLEAN: {
                final boolean replacingValue = nullReplacementValues.forBoolean();
                return new BooleanLiteralExpr(replacingValue);
            }
            case BYTE: {
                final byte replacingValue = nullReplacementValues.forByte();
                return new IntegerLiteralExpr(replacingValue);
            }
            case CHAR: {
                final char replacingValue = nullReplacementValues.forChar();
                return new CharLiteralExpr(replacingValue);
            }
            case SHORT: {
                final short replacingValue = nullReplacementValues.forShort();
                return new IntegerLiteralExpr(replacingValue);
            }
            case INT: {
                final int replacingValue = nullReplacementValues.forInt();
                return new IntegerLiteralExpr(replacingValue);
            }
            case LONG: {
                final long replacingValue = nullReplacementValues.forLong();
                return new LongLiteralExpr(replacingValue);
            }
            case FLOAT: {
                final float replacingValue = nullReplacementValues.forFloat();
                return new DoubleLiteralExpr(replacingValue);
            }
            case DOUBLE: {
                final double replacingValue = nullReplacementValues.forDouble();
                return new DoubleLiteralExpr(replacingValue);
            }
            default: throw new AssertionError(SpecializationProcessor.NOT_ALL_PRIMITIVES_HANDLED);
        }
    }

    private void acceptReturnStatements(final Node parentNode, final Stream.Builder<ReturnStmt> returnStmtBuilder) {
        for (final Node childNode : parentNode.getChildNodes()) {
            if (childNode instanceof ReturnStmt) {
                final ReturnStmt returnStmt = (ReturnStmt) childNode;
                returnStmtBuilder.accept(returnStmt);

                return;
            }

            acceptReturnStatements(childNode, returnStmtBuilder);
        }
    }

    private static boolean doesReturnNullReferenceType(final ReturnStmt returnStmt) {
        final Optional<Expression> optionalReturnedExpression = returnStmt.getExpression();

        if (optionalReturnedExpression.isPresent()) {
            Expression returnedExpression = optionalReturnedExpression.get();

            return returnedExpression.isNullLiteralExpr();
        }

        return false;
    }

    private static final String RUN_TIME_EXCEPTION_TYPE_QUALIFIED_NAME = IllegalArgumentException.class.getCanonicalName();

    private static final ClassOrInterfaceType RUN_TIME_EXCEPTION_TYPE = StaticJavaParser.parseClassOrInterfaceType(
        RUN_TIME_EXCEPTION_TYPE_QUALIFIED_NAME
    );

    private boolean updateReturnedExpression(
        final ReturnStmt returnStmt,
        final PrimitiveType returnedPrimitiveType
    ) {
        final NullReplacement nullReplacement = getReplacementConfiguration().nullReplacement();

        final NullReplacementType nullReplacementType = nullReplacement.nullReplacementType();
        final NullReplacementValues nullReplacementValues = nullReplacement.nullReplacementValues();

        switch (nullReplacementType) {
            case VALUE_REPLACEMENT: {
                final PrimitiveType.Primitive returnedPrimitive = returnedPrimitiveType.getType(); // TODO actually initialize

                final LiteralExpr replacingLiteral = newReplacingLiteral(returnedPrimitive, nullReplacementValues);

                returnStmt.setExpression(replacingLiteral);

                return true;
            }
            case COMPILE_TIME_EXCEPTION: {


                return false;
            }
            case RUN_TIME_EXCEPTION: {
                final ObjectCreationExpr exceptionCreator = new ObjectCreationExpr();
                exceptionCreator.setType(RUN_TIME_EXCEPTION_TYPE);

                final ThrowStmt exceptionThrower = new ThrowStmt(exceptionCreator);

                returnStmt.replace(exceptionThrower);

                return true;
            }
            default: throw new AssertionError("Not all possible `NullReplacementType`s are covered.");
        }
    }

    private Stream<ReturnStmt> findReturnStatements(final MethodDeclaration methodDeclaration) {
        Stream.Builder<ReturnStmt> returnStatementBuilder = Stream.builder();

        acceptReturnStatements(methodDeclaration, returnStatementBuilder);

        return returnStatementBuilder.build();
    }

    private void updateReturnExpressions(final MethodDeclaration methodDeclaration, final PrimitiveType primitiveMethodReturnType) {
        final boolean returnStmtsReplacementSuccess = findReturnStatements(methodDeclaration)
            .filter(SpecializationVisitor::doesReturnNullReferenceType)
            /* Note that this has a side effect of updating the return statements.
            The only situation where not every return statement is looked at is if
            a compile-time error is thrown instead of replacing the `return null;`. At that point,
            the replacing can end immediately.
             */
            .noneMatch(returnStmt -> this.updateReturnedExpression(returnStmt, primitiveMethodReturnType));

        if (!returnStmtsReplacementSuccess) {
            // TODO THROW COMPILE TIME ERROR
        }
    }

    @Override
    public Visitable visit(final MethodDeclaration methodDeclaration, final PrimitiveTypesCombination primitiveTypesCombination) {
        for (Parameter parameter : methodDeclaration.getParameters()) {
            getTypeSpecializationForwarder().updateVariableType(parameter, primitiveTypesCombination);
        }

        getTypeSpecializationForwarder().updateVariableType(methodDeclaration, primitiveTypesCombination);

        final Type updatedMethodReturnType = methodDeclaration.getType();

        updatedMethodReturnType.ifPrimitiveType(primitiveMethodReturnType -> updateReturnExpressions(methodDeclaration, primitiveMethodReturnType));

        return methodDeclaration;
    }

    private boolean valueAsLock = false;

    private boolean isValueAsLock() {
        return this.valueAsLock;
    }

    private void setValueAsLock() {
        this.valueAsLock = true;
    }

    @Getter
    private final String fullyAnnotatedClassName;

    @Getter
    private final ReplacementConfiguration replacementConfiguration;

    @Getter
    private NodeList<TypeParameter> annotatedElementTypeParameters;

    @Getter
    private final JavaParserFacade javaParserFacade;

    @Getter
    private final ClassToTypeMirror classToTypeMirror;

    private void removePrimitiveSpecializationsAnnotation(final ClassOrInterfaceDeclaration annotatedClassDeclaration) {
        // Guaranteed to be non-empty optional
        final Optional<AnnotationExpr> optionalPrimitiveSpecializationsAnnotation = annotatedClassDeclaration.getAnnotationByClass(
            PrimitiveSpecializations.class
        );

        final AnnotationExpr primitiveSpecializationsAnnotation = optionalPrimitiveSpecializationsAnnotation.orElseThrow(AssertionError::new);

        annotatedClassDeclaration.getAnnotations().remove(primitiveSpecializationsAnnotation);
    }

    private Visitable visitAnnotatedOldClass(
        final ClassOrInterfaceDeclaration annotatedDeclaration,
        final PrimitiveTypesCombination primitiveTypesCombination
    )   {
        final String primitiveSpecializationSimpleName = primitiveTypesCombination.getPreProcessedSpecializationClass();

        annotatedDeclaration.setName(primitiveSpecializationSimpleName);

        removePrimitiveSpecializationsAnnotation(annotatedDeclaration);

        final PrimitiveType[] primitiveTypes = primitiveTypesCombination.getPrimitiveTypes();
        final NodeList<TypeParameter> primitiveSpecializationTypeParameters = new NodeList<>();

        for (int typeParameterIndex = 0; typeParameterIndex < getAnnotatedElementTypeParameters().size(); typeParameterIndex++) {
            final PrimitiveType primitiveType = primitiveTypes[typeParameterIndex];

            if (primitiveType == null) {
                final TypeParameter annotatedElementTypeParameter = getAnnotatedElementTypeParameters().get(typeParameterIndex);

                primitiveSpecializationTypeParameters.add(annotatedElementTypeParameter);
            }
        }

        annotatedDeclaration.setTypeParameters(primitiveSpecializationTypeParameters);

        return annotatedDeclaration;
    }

    @Override
    public Visitable visit(
        final ClassOrInterfaceDeclaration classOrInterfaceDeclaration,
        final PrimitiveTypesCombination primitiveTypesCombination
    )   {
        final String observedClassSimpleName = classOrInterfaceDeclaration.getNameAsString();

        if (getFullyAnnotatedClassName().equals(observedClassSimpleName)) {
            return visitAnnotatedOldClass(classOrInterfaceDeclaration, primitiveTypesCombination);
        }

        return classOrInterfaceDeclaration;
    }

    /* private Optional<? extends Type> getImplicitScope(
        final ResolvedMethodDeclaration resolvedStaticMethodDeclaration,
        final PrimitiveTypesCombination primitiveTypesCombination
    ) {
        final ResolvedReferenceTypeDeclaration methodScopeDeclarationResolvedType = resolvedStaticMethodDeclaration.declaringType();

        return getTypeSpecializationForwarder().getStaticTypeSpecializer().getNewType(
            methodScopeDeclarationResolvedType,
            primitiveTypesCombination
        );
    } */

    private void handleMethodCallInitializingExpression(final MethodCallExpr methodCallExpr, final PrimitiveTypesCombination primitiveTypesCombination) {
        final MethodUsage initializingExpressionUsage = getJavaParserFacade().solveMethodAsUsage(methodCallExpr);

        final ResolvedMethodDeclaration resolvedInitializingExpressionDeclaration = initializingExpressionUsage.getDeclaration();

        // If the method call is not static, then we do not need to update the scope.
        if (resolvedInitializingExpressionDeclaration.isStatic()) {
            System.out.println("Fetching static scope");

            Optional<ClassOrInterfaceType> optionalNewType = getTypeSpecializationForwarder().getStaticTypeSpecializer().getNewType(initializingExpressionUsage, primitiveTypesCombination);

            optionalNewType.ifPresent(newType -> {
                final Expression newTypeAsExpression = newType.getNameAsExpression();

                methodCallExpr.setScope(newTypeAsExpression);
            });
        }
    }

    private void handleInitializingExpression(final Expression initializingExpression, final PrimitiveTypesCombination primitiveTypesCombination) {
        initializingExpression.ifMethodCallExpr(methodCallInitializingExpression -> handleMethodCallInitializingExpression(
            methodCallInitializingExpression,
            primitiveTypesCombination
        ));
    }

    @Override
    public Visitable visit(final VariableDeclarator variableDeclarator, final PrimitiveTypesCombination primitiveTypesCombination) {
        final Optional<Expression> optionalInitializingExpression = variableDeclarator.getInitializer();

        optionalInitializingExpression.ifPresent(initializingExpression -> handleInitializingExpression(
            initializingExpression,
            primitiveTypesCombination
        ));

        /* We update the actual type of the variable after its initializing expression is potentially updated
        because the potential update of the initializing expression is based on the pre-specialized type of the variable.

        Example:

        In the non-specialized class:
        `Value<K> stringValue = ValueCreator.create({instance of K});`

        In the specialized class:
        `IntValue intValue = IntValueCreator.create({an int});`

        If by the time the static specializer is reached the line looks like this:
        `IntValue intValue = ValueCreator.create({instance of K})`

        `SpecializationVisitor` would have to deduce the type arguments from `IntValue` in order to feed them
        into the replacement configuration for `ValueCreator`. It is much simpler to do the updating of the variable
        type after. */
        getTypeSpecializationForwarder().updateVariableType(variableDeclarator, primitiveTypesCombination);

        return variableDeclarator;
    }

    private boolean shouldAppendLockParameter() {
        final SynchronizationReplacementType synchronizationReplacementType = getReplacementConfiguration().synchronizationReplacementType();

        return isValueAsLock() && synchronizationReplacementType == SynchronizationReplacementType.APPEND_PARAMETER;
    }

    private static final Type LOCK_PARAMETER_TYPE = StaticJavaParser.parseClassOrInterfaceType(Object.class.getCanonicalName());

    @Override
    public Visitable visit(final ConstructorDeclaration constructorDeclaration, final PrimitiveTypesCombination primitiveTypesCombination) {
        final String preProcessedSpecializationName = primitiveTypesCombination.getPreProcessedSpecializationClass();

        constructorDeclaration.setName(preProcessedSpecializationName);

        if (shouldAppendLockParameter()) {
            constructorDeclaration.addParameter(LOCK_PARAMETER_TYPE, APPENDED_LOCK_NAME);
        }

        return constructorDeclaration;
    }

    private static final String APPENDED_LOCK_NAME = "lock";

    @Override
    public Visitable visit(final CastExpr castExpr, final PrimitiveTypesCombination primitiveTypesCombination) {
        final Expression castSubject = castExpr.getExpression();

        return castExpr;
    }

    @Override
    public Visitable visit(final SynchronizedStmt synchronizedStatement, final PrimitiveTypesCombination primitiveTypesCombination) {
        final Expression oldLock = synchronizedStatement.getExpression();

        ResolvedType resolvedType = oldLock.calculateResolvedType();

        if (resolvedType.isPrimitive()) {
            switch (getReplacementConfiguration().synchronizationReplacementType()) {
                case FORBID: {

                    break;
                }
                case APPEND_PARAMETER: {
                    setValueAsLock();

                    final NameExpr newLock = new NameExpr(APPENDED_LOCK_NAME);

                    synchronizedStatement.setExpression(newLock);
                    break;
                }
            }
        }

        return synchronizedStatement;
    }

    @Override
    public Visitable visit(final MethodCallExpr methodCallExpr, final PrimitiveTypesCombination primitiveTypesCombination) {
        final Optional<Expression> optionalMethodCaller = methodCallExpr.getScope();

        if (optionalMethodCaller.isPresent()) {
            final Expression methodCaller = optionalMethodCaller.get();

            // Is an instance method
            try {
                final ResolvedType variableType = getJavaParserFacade().getType(methodCaller, true);

                return getMethodCallExprVisitorForwarder().visitInstanceInitializingMethodCallExpr(methodCallExpr, variableType, primitiveTypesCombination);
            }
            // Else is a static method
            catch (final UnsolvedSymbolException e) {
                try {
                    final ResolvedType type = getJavaParserFacade().getType(methodCaller);

                    return getMethodCallExprVisitorForwarder().visitStaticInitializingMethodCallExpr(methodCallExpr, type, primitiveTypesCombination);
                }
                catch (final UnsolvedSymbolException ignored) {}
            }
        }

        return methodCallExpr;
    }
}
