package io.github.sandydunlop.markista.model;

public class OverriddenMethodNode {
    /// Fully qualified name of the class that declares the overridden method. 
    private String qualifiedClassName = "";

    /// Name of the overridden method.
    private String methodName = "";

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

    /// Returns the name of the overridden method.
    /// @return The method name.
    public String getMethodName() {
        return methodName;
    }
}
