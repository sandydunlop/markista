package io.github.sandydunlop.markista.model;

/// Represents a Java class.
/// This class is intended to model a class within a program's structure.
/// It inherits properties from TypeNode and specifies its own kind.
public class ClassTypeNode extends TypeNode {
    /// Constructs a new ClassTypeNode.
    /// @param qualifiedName The fully qualified name of the class.
    /// @param simpleName The simple name of the class.
    /// @param packageName The name of the package this class belongs to.
    public ClassTypeNode(String qualifiedName, String simpleName, String packageName) {
        super(qualifiedName, simpleName, packageName);
        kind = NodeKind.CLASS;
    }
}
