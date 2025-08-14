package io.github.sandydunlop.markista.model;

public abstract class ModuleMemberNode extends Node {
    /// The simple form of the node's name
    protected String simpleName = "";

    /// The canonical form of the type's name
    protected String qualifiedName;
    
    /// A [PackageNode] representing the package the node belongs to
    protected PackageNode packageNode = null;

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

    /// Sets the qualified name of this type.
    /// @param name the qualified name to set.
    public void setQualifiedName(String name) {
        qualifiedName = name;
    }

    /// Returns the qualified name of this type.
    /// @return the qualified name.
    public String getQualifiedName() {
        return qualifiedName;
    }
}
