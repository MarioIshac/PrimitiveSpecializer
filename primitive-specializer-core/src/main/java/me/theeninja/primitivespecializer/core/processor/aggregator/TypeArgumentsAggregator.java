package me.theeninja.primitivespecializer.core.processor.aggregator;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;
import lombok.AccessLevel;
import lombok.Getter;
import me.theeninja.primitivespecializer.core.annotation.FormattedClassSpecifier;
import me.theeninja.primitivespecializer.core.processor.PrimitiveTypesCombination;

/*
    There are 3 groups of type arguments that, depending on their contents, change the replacing (new) class for the old class.

    1) Specialized, generic type parameters.
    2) Not specialized, generic, type parameters.
    3) Not specialized, non-generic, type parameters. For example,
       in List<A>, A is NOT a predetermined, non-generic type, but in List<Integer>, Integer IS a predetermined, generic type.

    For the example of `Tuple<K, V, KeyInfo>`, assuming we wanted to specialize the key for a certain primitive type, such as an integer,
    the replacing class would be something like `IntTuple<V, KeyInfo>`. This is because:

    `K` is in group 1, it is a specialized, generic, type parameter.
    `V` is in group 2, it is a not-specialized, generic, type argument.
    `KeyInfo` is in group 3, it is a non-specialized, non-generic, type argument.

    Fortunately, group 2 and 3 can be grouped into category: not removing the type argument.
    Likewise, group 1 yields a removal of the type argument
*/
@Getter(AccessLevel.PACKAGE)
public abstract class TypeArgumentsAggregator {
    private final PrimitiveTypesCombination primitiveTypesCombination;

    private final NodeList<PrimitiveType> specializationsOfRemovedTypeArguments = new NodeList<>();

    public TypeArgumentsAggregator(final PrimitiveTypesCombination primitiveTypesCombination) {
        this.primitiveTypesCombination = primitiveTypesCombination;
    }

    public abstract void aggregate(final ResolvedType resolvedTypeArgument);

    public abstract String newSimpleClassName(final FormattedClassSpecifier formattedClassSpecifier, String oldSimpleClassName);
    public abstract NodeList<Type> getNewTypeArguments();
}
