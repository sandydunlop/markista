package io.github.sandydunlop.markista.model;

/// A class to hold information about method parameters.
public class ParamNode extends Node {
    private TypeNode type = null;

    /// Constructs a ParamNode with the given type and name.
    /// @param type The TypeNode representing the parameter's type.
    /// @param name The simple name of the parameter.
    public ParamNode(TypeNode type, String name) {
        this.type = type;
        this.simpleName = name;
    }

    /// Returns the type of this parameter.
    /// @return The TypeNode representing the parameter's type.
    public TypeNode getType() {
        return type;
    }

    /// Sets the type of this parameter.
    /// @param type The TypeNode to set as this parameter's type.
    public void setType(TypeNode type) {
        this.type = type;
    }

    /// Sets the simple name of this parameter.
    /// @param name The simple name to set.
    public void setSimpleName(String name) {
        simpleName = name;
    }

    /// Returns the simple name of this parameter.
    /// @return The simple name.
    public String getSimpleName() {
        return simpleName;
    }

    /// Sets the qualified name of this parameter.
    /// @param name The qualified name to set.
    public void setQualifiedName(String name) {
        qualifiedName = name;
    }

    /// Returns the qualified name of this parameter.
    /// @return The qualified name.
    public String getQualifiedName() {
        return qualifiedName;
    }
}
