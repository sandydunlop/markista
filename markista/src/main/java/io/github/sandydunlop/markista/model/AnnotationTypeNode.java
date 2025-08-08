package io.github.sandydunlop.markista.model;

/// A [Node] that represents an [Annotation](java.text.Annotation) class 
public class AnnotationTypeNode extends TypeNode {
    /// Constructor that sets the minimum required information for an AnnotationTypeNode.
    /// @param qualifiedName the canonical name of the Annotation
    /// @param simpleName the unqualified name of the Annotation
    /// @param packageNode the [PackageNode] representing the package that the annotation is a member of
    public AnnotationTypeNode(String qualifiedName, String simpleName, PackageNode packageNode) {
        super(qualifiedName, simpleName, packageNode);
        kind = TypeNode.Kind.ANNOTATION;
    }
}

