package io.github.sandydunlop.markista.model;

/// A node representing a Java interface type
public class InterfaceTypeNode extends TypeNode {

    /// Create an InterfaceTypeNode with the specified details
    /// @param qualifiedName the canonical name of the Annotation
    /// @param simpleName The simple name of the enum.
    /// @param packageNode the [PackageNode] representing the package that the interface is a member of
    public InterfaceTypeNode(String qualifiedName, String simpleName, PackageNode packageNode) {
        super(qualifiedName, simpleName, packageNode);
        kind = TypeNode.Kind.INTERFACE;
    }
}
