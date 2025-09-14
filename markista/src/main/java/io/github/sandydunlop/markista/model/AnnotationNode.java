package io.github.sandydunlop.markista.model;

/// A [Node] that represents an [Annotation](java.text.Annotation) class
public class AnnotationNode extends TypeNode {
    /// Constructor that sets the minimum required information for an AnnotationNode.
    /// @param name the name of the Annotation
    public AnnotationNode(Name name) {
        super(name);
        kind = Node.Kind.ANNOTATION;
    }
}

