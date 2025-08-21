package io.github.sandydunlop.markista.model;

/// Represents a Java record class.
/// This class is intended to model a class within a program's structure.
/// It inherits properties from TypeNode and specifies its own kind.
public class RecordTypeNode extends TypeNode {
    /// Constructs a new RecordTypeNode.
    /// @param qualifiedName The fully qualified name of the record.
    /// @param simpleName The simple name of the record.
    /// @param packageName The name of the package this record belongs to.
    public RecordTypeNode(String qualifiedName, String simpleName, String packageName) {
        super(qualifiedName, simpleName, packageName);
        this.kind = NodeKind.RECORD;
    }
}
