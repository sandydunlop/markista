package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

/// Represents an enum type node with its constants.
public class EnumNode extends TypeNode {
    private final List<FieldNode> constants = new ArrayList<>();

    /// Constructs an EnumNode with the specified simple name and package.
    /// Sets the kind to ENUM.
    /// @param simpleName The simple name of the enum.
    /// @param packageName The name of the package that contains this enum.
    public EnumNode(String simpleName, String packageName) {
        super(simpleName, packageName);
        kind = Node.Kind.ENUM;
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
