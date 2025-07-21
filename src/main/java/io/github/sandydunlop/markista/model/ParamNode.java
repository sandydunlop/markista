package io.github.sandydunlop.markista.model;

/// A class to hold infomation about method parameters.
public class ParamNode extends Node {
    private TypeNode type = null;

    public ParamNode(TypeNode type, String name) {
        this.type = type;
        this.simpleName = name;
    }

    public TypeNode getType() {
        return type;
    }

    public void setType(TypeNode type) {
        this.type = type;
    }

    public void setSimpleName(String name) {
        simpleName = name;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public void setQualifiedName(String name) {
        qualifiedName = name;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }
}
