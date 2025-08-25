package io.github.sandydunlop.markista.model;

/// A [Node] that represents an [Annotation](java.text.Annotation) class 
public class AnnotationNode extends TypeNode {
    /// Constructor that sets the minimum required information for an AnnotationNode.
    /// @param simpleName the unqualified name of the Annotation
    /// @param packageName the name of the package that the annotation is a member of
    public AnnotationNode(String simpleName, String packageName) {
        super(simpleName, packageName);
        kind = Node.Kind.ANNOTATION;
    }
}

