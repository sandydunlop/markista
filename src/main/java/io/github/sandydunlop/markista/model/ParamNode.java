package io.github.sandydunlop.markista.model;

/// A class to hold infomation about method parameters.
public class ParamNode extends Node {
    public TypeNode type = null;
    public ParamNode(){}
    
    public ParamNode(TypeNode type, String name) {
        this.type = type;
        this.simpleName = name;
    }
}
