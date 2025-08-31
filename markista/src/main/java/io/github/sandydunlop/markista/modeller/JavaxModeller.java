package io.github.sandydunlop.markista.modeller;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.TypeNode;

public class JavaxModeller implements Modeller<TypeElement, VariableElement, ExecutableElement, VariableElement> {

    @Override
    public TypeNode modelType(TypeElement type) {
        return null;
    }

    @Override
    public FieldNode modelField(VariableElement field) {
        return null;
    }

    @Override
    public MethodNode modelMethod(ExecutableElement method) {
        return null;
    }

    @Override
    public ParamNode modelParam(VariableElement param) {
        return null;
    }
}
