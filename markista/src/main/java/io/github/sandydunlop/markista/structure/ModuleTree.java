package io.github.sandydunlop.markista.structure;

import java.util.ArrayList;

import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageMember;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.TypeNode;

public class ModuleTree extends AbstractTree {
    public ModuleTree() {
        // Nothing to see here
    }

    public void scan() {
        y = 0;
        contents = new ArrayList<>();
        root = addModule(module, null);
        for (TreeNode treeNode : contents) {
            if (treeNode.getLabel().length() > imageWidth) {
                imageWidth = treeNode.getLabel().length() ;
            }
        }
        imageWidth = (imageWidth + 2) * 12;
        imageHeight = y;
    }

    private TreeNode addModule(ModuleNode node, TreeNode current) {
        String name = node.getName().isBlank() ? "unnamed module" : node.getName();
        TreeNode treeNode = new TreeNode(NodeKind.MODULE, name);
        if (current != null) {
            current.addChild(treeNode);
            treeNode.setX(current.getX() + indent);
            treeNode.setY(y);
            treeNode.setParent(current);
        }
        y += lineHeight;
        contents.add(treeNode);
        for (PackageMember pkg : node.getPackages()) {
            if (pkg instanceof PackageNode pn) {
                addPackage(pn, treeNode);
            }
        }
        if (node.hasModuleInfo()) {
            TypeNode tn = new TypeNode("", "module-info", null);
            addType(tn, treeNode);
        }
        return treeNode;
    }

    private void addPackage(PackageNode node, TreeNode current) {
        TreeNode treeNode = new TreeNode(NodeKind.PACKAGE, node.getName());
        if (current != null) {
            current.addChild(treeNode);
            treeNode.setX(current.getX() + indent);
            treeNode.setY(y);
            treeNode.setParent(current);
        }
        y += lineHeight;
        contents.add(treeNode);
        for (PackageMember pkg : node.getPackages()) {
            if (pkg instanceof PackageNode pn) {
                addPackage(pn, treeNode);
            }
        }
        if (node.hasPackageInfo()) {
            TypeNode tn = new TypeNode("", "package-info", node);
            addType(tn, treeNode);
        }
        for (TypeNode type : node.getTypes()) {
            addType(type, treeNode);
        }
    }

    private void addType(TypeNode node, TreeNode current) {
        TreeNode treeNode = new TreeNode(NodeKind.CODE, node.getSimpleName());
        if (current != null) {
            current.addChild(treeNode);
            treeNode.setX(current.getX() + indent);
            treeNode.setY(y);
            treeNode.setParent(current);
        }
        y += lineHeight;
        contents.add(treeNode);
    }
}
