package io.github.sandydunlop.markista.model;

public class ClassNode extends TypeNode {
    public ClassNode(String qualifiedName, String simpleName, PackageNode packageNode) {
        super(qualifiedName, simpleName, packageNode);
        kind = TypeNode.Kind.CLASS;
    }
}
