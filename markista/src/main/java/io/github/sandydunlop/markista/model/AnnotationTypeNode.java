package io.github.sandydunlop.markista.model;

/// A [Node] that represents an [Annotation](java.text.Annotation) class 
public class AnnotationTypeNode extends TypeNode {
    /// Constructor that sets the minimum required information for an AnnotationTypeNode.
    /// @param qualifiedName the canonical name of the Annotation
    /// @param simpleName the unqualified name of the Annotation
    /// @param packageName the name of the package that the annotation is a member of
    public AnnotationTypeNode(String qualifiedName, String simpleName, String packageName) {
        super(qualifiedName, simpleName, packageName);
        kind = TypeNode.Kind.ANNOTATION;
    }
}

