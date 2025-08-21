package io.github.sandydunlop.markista.model;

/// An element of an annotation
public class AnnotationElement extends Node {
    private final String value;

    /// The simple form of the name
    protected String simpleName = "";

    protected String typeName = "";

    /// Constructs an annotation element
    /// @param typeName The type of the annotation element
    /// @param name The name of the annotation element
    /// @param value The value of the annotation element
    public AnnotationElement(String typeName, String name, String value) {
        this.typeName = typeName;
        this.simpleName = name;
        this.value = value;
    }

    /// Sets the simple name of this type.
    /// @param name the simple name to set.
    public void setSimpleName(String name) {
        simpleName = name;
    }

    /// Returns the simple name of this type.
    /// @return the simple name.
    public String getSimpleName() {
        return simpleName;
    }

    /// Gets the value of this annotation element
    /// @return The value of the element
    public String getValue() {
        return this.value;
    }
    
    /// Returns the type of this element.
    /// @return The name of the element's type.
    public String getTypeName() {
        return typeName;
    }

    /// Sets the type of this element.
    /// @param type The name of this element's type.
    public void setTypeName(String type) {
        this.typeName = type;
    }
}
