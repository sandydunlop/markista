package io.github.sandydunlop.markista.model;

import java.io.Serializable;

/// A class to hold infomation about fields within a class, interface, or enum.
public class FieldNode extends ParamNode {
    public Serializable constantValue = null;

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
