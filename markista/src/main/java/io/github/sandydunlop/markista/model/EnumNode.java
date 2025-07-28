package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

public class EnumNode extends TypeNode {
    private List<FieldNode> constants = new ArrayList<>();

    public EnumNode(String qualifiedName, String simpleName, PackageNode packageNode) {
        super(qualifiedName, simpleName, packageNode);
        kind = TypeNode.Kind.ENUM;
    }

    public void addConstant(FieldNode constant) {
        constants.add(constant);
    }

    public List<FieldNode> getConstants() {
        return constants;
    }
}
