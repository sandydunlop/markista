package io.github.sandydunlop.markista.model;

/// A class to hold infomation about method parameters.
public class ParamNode extends Node {
    public TypeNode type = null;
    public ParamNode(){}
    
    public ParamNode(TypeNode type, String name) {
        this.type = type;
        this.simpleName = name;
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
