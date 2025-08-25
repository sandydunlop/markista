package io.github.sandydunlop.markista.model;

/// Represents a Java class.
/// This class is intended to model a class within a program's structure.
/// It inherits properties from TypeNode and specifies its own kind.
public class ClassNode extends TypeNode {
    /// Constructs a new ClassNode.
    /// @param simpleName The simple name of the class.
    /// @param packageName The name of the package this class belongs to.
    public ClassNode(String simpleName, String packageName) {
        super(simpleName, packageName);
        kind = Node.Kind.CLASS;
    }
}
