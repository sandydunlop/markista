package io.github.sandydunlop.markista.model;

public class InterfaceNode extends TypeNode {
    public InterfaceNode(String qualifiedName, String simpleName, PackageNode packageNode) {
        super(qualifiedName, simpleName, packageNode);
        kind = TypeNode.Kind.INTERFACE;
    }
}
