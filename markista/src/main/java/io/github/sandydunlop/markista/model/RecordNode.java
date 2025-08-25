package io.github.sandydunlop.markista.model;

/// Represents a Java record class.
/// This class is intended to model a class within a program's structure.
/// It inherits properties from TypeNode and specifies its own kind.
public class RecordNode extends TypeNode {
    /// Constructs a new RecordNode.
    /// @param simpleName The simple name of the record.
    /// @param packageName The name of the package this record belongs to.
    public RecordNode(String simpleName, String packageName) {
        super(simpleName, packageName);
        this.kind = Node.Kind.RECORD;
    }
}
