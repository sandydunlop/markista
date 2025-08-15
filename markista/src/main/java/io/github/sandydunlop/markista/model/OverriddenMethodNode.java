package io.github.sandydunlop.markista.model;

import java.io.Serial;
import java.io.Serializable;

/// Represents a method from another type that is overridden by a method in the type being documented.
public class OverriddenMethodNode implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /// Fully qualified name of the class that declares the overridden method. 
    private final String qualifiedClassName;

    private Text text = Text.empty();

    /// Name of the overridden method.
    private final String methodName;

    private Reference reference;

    /// Constructs an OverriddenMethodNode with specified qualified class name and method name.
    /// @param qualifiedClassName The fully qualified class name.
    /// @param methodName The method name.
    public OverriddenMethodNode(String qualifiedClassName, String methodName) {
        this.qualifiedClassName = qualifiedClassName;
        this.methodName = methodName;
    }

    /// Returns the fully qualified class name of the overridden method.
    /// @return The qualified class name.
    public String getClassName() {
        return qualifiedClassName;
    }

    public void setReference(Reference ref) {
        reference = ref;
    }

    public Reference getReference() {
        return reference;
    }

    /// Returns the name of the overridden method.
    /// @return The method name.
    public String getMethodName() {
        return methodName;
    }

    public void setText(Text text) {
        this.text = text;
    }

    public Text getText() {
        return text;
    }
}
