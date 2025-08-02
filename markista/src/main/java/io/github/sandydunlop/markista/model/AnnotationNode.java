package io.github.sandydunlop.markista.model;

/// A [Node] that represents an [Annotation](java.text.Annotation) class 
public class AnnotationNode extends TypeNode {
    /// Constructor that sets the minimum required information for an `AnnotationNode`.
    /// @param qualifiedName the canonical name of the Annotation
    /// @param simpleName the unqualified name of the Annotation
    /// @param packageNode the PackageNode representing the package that the Annotation is a member of
    public AnnotationNode(String qualifiedName, String simpleName, PackageNode packageNode) {
        super(qualifiedName, simpleName, packageNode);
        kind = TypeNode.Kind.ANNOTATION;
    }
}

