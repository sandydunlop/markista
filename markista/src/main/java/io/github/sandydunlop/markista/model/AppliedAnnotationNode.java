package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

/// An annotation applied to a type
public class AppliedAnnotationNode extends Node {
    private boolean custom = false;
    private boolean documented = false;
    private TypeNode type;
    private final List<AnnotationElement> elements = new ArrayList<>();

    /// Constructs an AppliedAnnotationNode
    public AppliedAnnotationNode() {
        // Only here for the comments
    }

    /// Constructs an AppliedAnnotationNode with the given type
    /// @param type The type of this annotation
    public AppliedAnnotationNode(TypeNode type) {
        this.type = type;
    }


    /// Specifies if this annotation will be treated as custom or built-in.
    /// @param b If true, set that this annotation is a custom one
    public void setCustom(boolean b) {
        custom = b;
    }

    /// Indicates if this is a custom annotation
    /// @return True if the annotation is a custom one, otherwise false
    public boolean isCustom() {
        return custom;
    }

    /// Sets the documented flag, indicating that this annotation should
    /// be displayed in the documentation of types or fields that it is
    /// applied to.
    /// @param b If true, this annotation will appear in the documentation
    /// of types and fields it is applied to.
    public void setDocumented(boolean b) {
        documented = b;
    }

    /// Gets the flag the indicates this annotation should appear in
    /// the documentation of the type or field it is applied to.
    /// @return True if this annotation should appear in such documentation.
    public boolean isDocumented() {
        return documented;
    }

    /// Returns the type of this annotation.
    /// @return The TypeNode representing the annotation's type.
    public TypeNode getType() {
        return type;
    }

    /// Sets the type of this annotation.
    /// @param type The TypeNode to set as this annotation's type.
    public void setType(TypeNode type) {
        this.type = type;
    }

    /// Adds an element to this annotation
    /// @param element the element to add to this annotation
    public void addElement(AnnotationElement element) {
        elements.add(element);
    }

    /// Returns the list of elements of this annotation.
    /// @return list of elements belonging to this annotation.
    public List<AnnotationElement> getElements() {
        return elements;
    }
}
