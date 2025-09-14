package io.github.sandydunlop.markista.model;

/// Represents a Java record class.
/// This class is intended to model a class within a program's structure.
/// It inherits properties from TypeNode and specifies its own kind.
public class RecordNode extends TypeNode {
    /// Constructs a new RecordNode.
    /// @param name The name of the record.
    public RecordNode(Name name) {
        super(name);
        this.kind = Node.Kind.RECORD;
    }
}
