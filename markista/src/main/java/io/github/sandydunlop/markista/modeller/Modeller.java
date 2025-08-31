package io.github.sandydunlop.markista.modeller;

import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.TypeNode;

public interface Modeller<C,F,M,P> {
    public TypeNode modelType(C type);
    public FieldNode modelField(F field);
    public MethodNode modelMethod(M method);
    public ParamNode modelParam(P param);
}
