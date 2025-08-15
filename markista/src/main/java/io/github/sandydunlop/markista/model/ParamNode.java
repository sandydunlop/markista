package io.github.sandydunlop.markista.model;

/// A class to hold information about method parameters.
public class ParamNode extends AbstractPackageMember {
    /// The type of this parameter
    private String typeName;
    private Text typeText = Text.empty();

    /// Constructs a ParamNode with the given type and name.
    /// @param type The TypeNode representing the parameter's type.
    /// @param name The simple name of the parameter.
    public ParamNode(String type, String name) {
        this.typeName = type;
        this.simpleName = name;
    }

    /// Returns the type of this parameter.
    /// @return The name of the parameter's type.
    public String getTypeName() {
        return typeName;
    }

    /// Sets the type of this parameter.
    /// @param type The name of this parameter's type.
    public void setTypeName(String type) {
        this.typeName = type;
    }

    /// Returns the type Text of this parameter.
    /// @return The Text representing the parameter's type.
    public Text getTypeText() {
        return typeText;
    }

    /// Sets the type Text of this parameter.
    /// @param text The Text to set as this parameter's type Text.
    public void setTypeText(Text text) {
        this.typeText = text;
    }
}
