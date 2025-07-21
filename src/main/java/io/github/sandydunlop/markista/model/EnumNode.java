package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

public class EnumNode extends TypeNode {
    public List<FieldNode> constants = new ArrayList<>();
    public EnumNode(String qualifiedName, String simpleName, PackageNode packageNode) {
        super(qualifiedName, simpleName, packageNode);
        kind = TypeNode.Kind.ENUM;
    }
}
