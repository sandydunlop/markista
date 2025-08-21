package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

/// Represents an enum type node with its constants.
public class EnumTypeNode extends TypeNode {
    private final List<FieldNode> constants = new ArrayList<>();

    /// Constructs an EnumNode with the specified qualified name, simple name, and package.
    /// Sets the kind to ENUM.
    /// @param qualifiedName The qualified name of the enum.
    /// @param simpleName The simple name of the enum.
    /// @param packageName The name of the package that contains this enum.
    public EnumTypeNode(String qualifiedName, String simpleName, String packageName) {
        super(qualifiedName, simpleName, packageName);
        kind = NodeKind.ENUM;
    }

    /// Adds a constant field to this enum.
    /// @param constant The FieldNode representing the enum constant.
    public void addConstant(FieldNode constant) {
        constants.add(constant);
    }

    /// Returns the list of enum constants.
    /// @return List of FieldNode constants.
    public List<FieldNode> getConstants() {
        return constants;
    }
}
