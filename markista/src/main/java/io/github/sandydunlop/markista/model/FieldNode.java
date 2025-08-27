package io.github.sandydunlop.markista.model;

import java.io.Serializable;

/// A class to hold information about fields within a class, interface, or enum.
public class FieldNode extends ParamNode { 

    /// The constant value assigned to this field, if any. 
    private Serializable constantValue = null;

    private TypeReference constantValueReference = null;

    /// Constructs a FieldNode with the given type and name.
    /// @param type The type of the field.
    /// @param name The simple name of the field.
    public FieldNode(String type, String name) {
        super(type, name);
    }

    /// Returns the constant value of this field.
    /// @return The constant value of the field or null if none.
    public Serializable getConstantValue() {
        return constantValue;
    }

    /// Sets the constant value of this field.
    /// @param constantValue The constant value to set.
    public void setConstantValue(Serializable constantValue) {
        this.constantValue = constantValue;
    }

    public void setConstantValueReference(TypeReference ref) {
        constantValueReference = ref;
    }

    public TypeReference getConstantValueReference() {
        return constantValueReference;
    }

    /// Returns the full signature of the field including modifiers and name.
    /// @return The string representing the full signature.
    public String fullSignature() {
        String sig = getModifiersString();
        if (!sig.isEmpty()) {
            sig += " ";
        }
        sig += simpleName;
        return sig;
    }
}
