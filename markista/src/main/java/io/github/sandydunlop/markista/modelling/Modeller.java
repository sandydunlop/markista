package io.github.sandydunlop.markista.modelling;

import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.TypeNode;

public interface Modeller<A,B,C,F,M,P> {
    public ModuleNode modelModule(A m);
    public PackageNode modelPackage(B p);
    public TypeNode modelType(C t);
    public ClassNode modelClass(C t);
    public FieldNode modelField(F f);
    public MethodNode modelMethod(M m);
    public ParamNode modelParam(P p);
}

