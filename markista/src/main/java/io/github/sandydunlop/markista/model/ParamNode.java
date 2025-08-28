package io.github.sandydunlop.markista.model;

/// A class to hold information about method parameters.
public class ParamNode extends AbstractMember {
    /// The type of this parameter
    private TypeReference type;

    /// Constructs a ParamNode with the given type and name.
    /// @param type The TypeNode representing the parameter's type.
    /// @param name The simple name of the parameter.
    public ParamNode(String type, String name) {
        this.type = TypeReference.to(type);
        this.simpleName = name;
    }

    /// Returns the type Text of this parameter.
    /// @return The Text representing the parameter's type.
    public TypeReference getType() {
        return type;
    }

    /// Sets the type Text of this parameter.
    /// @param type The Text to set as this parameter's type Text.
    public void setType(TypeReference type) {
        this.type = type;
    }
}
