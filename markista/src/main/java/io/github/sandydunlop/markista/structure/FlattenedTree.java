package io.github.sandydunlop.markista.structure;

import java.util.ArrayList;

import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageMember;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.TypeNode;

public class FlattenedTree extends AbstractTree {
    public FlattenedTree() {
        // Nothing to see here
    }

    public void scan() {
        y = 0;
        contents = new ArrayList<>();
        root = addModule(module, null);
        imageHeight = contents.size() * lineHeight;
        for (TreeNode treeNode : contents) {
            if (treeNode.getLabel().length() > imageWidth) {
                imageWidth = treeNode.getLabel().length() ;
            }
        }
        imageWidth = (imageWidth + 2) * 12;
        imageHeight = y;
    }

    private TreeNode addModule(ModuleNode node, TreeNode current) {
        TreeNode treeNode = new TreeNode(SvgIcon.module(), node.getName());
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
        return treeNode;
    }

    private void addPackage(PackageNode node, TreeNode current) {
        TreeNode treeNode = new TreeNode(SvgIcon.pkg(), node.getName());
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
        for (TypeNode type : node.getTypes()) {
            addType(type, treeNode);
        }
    }

    private void addType(TypeNode node, TreeNode current) {
        TreeNode treeNode = new TreeNode(SvgIcon.code(), node.getSimpleName());
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
