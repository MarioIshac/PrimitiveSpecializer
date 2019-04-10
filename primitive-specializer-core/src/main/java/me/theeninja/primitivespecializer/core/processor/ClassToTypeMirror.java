package me.theeninja.primitivespecializer.core.processor;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.Set;
import java.util.function.Supplier;

@AllArgsConstructor
@Getter(AccessLevel.PRIVATE)
class ClassToTypeMirror {
    private final Elements elements;

    DeclaredType convert(final Supplier<Class<?>> classGetter) {
        try {
            final Class<?> suppliedClass = classGetter.get();

            final Module classModule = suppliedClass.getModule();
            final String classModuleName = classModule.getName();
            final String classQualifiedName = suppliedClass.getCanonicalName();

            Set<? extends TypeElement> potentialEquivalentTypeElements = getElements().getAllTypeElements(classQualifiedName);

            for (final TypeElement potentialEquivalentTypeElement : potentialEquivalentTypeElements) {
                ModuleElement potentialEquivalentTypeElementModule = getElements().getModuleOf(potentialEquivalentTypeElement);

                Name potentialEquivalentTypeElementModuleName = potentialEquivalentTypeElementModule.getQualifiedName();

                if (potentialEquivalentTypeElementModuleName.contentEquals(classModuleName)) {
                    final TypeMirror equivalentTypeMirror = potentialEquivalentTypeElement.asType();

                    return (DeclaredType) equivalentTypeMirror;
                }
            }
        }
        catch(final MirroredTypeException e) {
            final TypeMirror associatedTypeMirror = e.getTypeMirror();
            return (DeclaredType) associatedTypeMirror;
        }

        throw new AssertionError("Annotation Processor's access to `suppliedClass` is different " +
                                 "from the access of the context in which `suppliedClass` was declared.");
    }
}
