package io.github.sandydunlop.markista.model;

/// An element of an annotation
public class AnnotationElement extends ParamNode {
    private final String value;

    /// Constructs an annotation element
    /// @param type The type of the annotation element
    /// @param name The name of the annotation element
    /// @param value The value of the annotation element
    public AnnotationElement(TypeNode type, String name, String value) {
        super(type, name);
        this.value = value;
    }


    /// Gets the value of this annotation element
    /// @return The value of the element
    public String getValue() {
        return this.value;
    }
    
}
