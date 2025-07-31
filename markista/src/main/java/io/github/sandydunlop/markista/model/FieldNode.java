package io.github.sandydunlop.markista.model;

import java.io.Serializable;

/// A class to hold information about fields within a class, interface, or enum.
public class FieldNode extends ParamNode {
    private Serializable constantValue = null;

    public Serializable getConstantValue() {
        return constantValue;
    }

    public void setConstantValue(Serializable constantValue) {
        this.constantValue = constantValue;
    }

    public FieldNode(TypeNode type, String name) {
        super(type, name);
    }

    public String fullSignature() {
        String sig = getModifiersString();
        if (!sig.isEmpty()) sig += " ";
        sig += simpleName;
        return sig;
    }
}
