package io.github.sandydunlop.markista.model;

public class AnnotationNode extends TypeNode {
    public AnnotationNode(String qualifiedName, String simpleName, PackageNode packageNode) {
        super(qualifiedName, simpleName, packageNode);
        kind = TypeNode.Kind.ANNOTATION;
    }
}

