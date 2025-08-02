package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

/// Represents an enum type node with its constants.
public class EnumNode extends TypeNode {
    private List<FieldNode> constants = new ArrayList<>();

    /// Constructs an EnumNode with the specified qualified name, simple name, and package.
    /// Sets the kind to ENUM.
    /// @param qualifiedName The qualified name of the enum.
    /// @param simpleName The simple name of the enum.
    /// @param packageNode The PackageNode that contains this enum.
    public EnumNode(String qualifiedName, String simpleName, PackageNode packageNode) {
        super(qualifiedName, simpleName, packageNode);
        kind = Kind.ENUM;
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
