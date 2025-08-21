package io.github.sandydunlop.markista.model;

/// Enumeration representing kinds of types: None, Class, Interface, Enum, Annotation.
public enum NodeKind {
    /// No type has been set
    NONE ("None"),

    /// A class, including abstract class and exception class
    CLASS ("Class"),

    /// An interface
    INTERFACE ("Interface"),

    /// A record
    RECORD ("Record Class"),

    /// An enum
    ENUM ("Enum Class"),

    /// An annotation
    ANNOTATION ("Annotation Interface"),

    /// An annotation
    FIELD ("Field"),

    /// An annotation
    METHOD ("Method"),

    /// An annotation
    PARAMETER ("Parameter");

    /// The display name for the kind.
    private final String name;

    /// Constructor assigning the display name.
    NodeKind(String s) {
        name = s;
    }

    /// Returns the display name of the kind.
    @Override
    public String toString() {
        return this.name;
    }
}
