package io.github.sandydunlop.markista.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.sandydunlop.markista.structure.SvgIcon.Kind;

public class RegularTree extends AbstractTree {
    public RegularTree() {
        // Nothing to see here
    }

    public void build(TreeNode moduleRoot, List<TreeNode> moduleContents) {
        y = 0;
        root = new TreeNode(moduleRoot.getIcon(), moduleRoot.getLabel());
        contents = new ArrayList<>();
        contents.add(root);
        y += lineHeight;
        for (TreeNode node : moduleContents) {
            if (node.getIcon().getKind() == Kind.CODE) {
                if (node != moduleRoot) {
                    String packageName = node.getParent().getLabel();
                    List<String> list = new ArrayList<>(Arrays.asList(packageName.split("\\.")));
                    TreeNode branch = getBranch(root, list);
                    TreeNode leaf = new TreeNode(node.getIcon(), node.getLabel());
                    leaf.setX(branch.getX() + indent);
                    leaf.setY(y);
                    leaf.setParent(branch);
                    branch.addChild(leaf);
                    contents.add(leaf);
                    y += lineHeight;
                }
            }
        }
        for (TreeNode treeNode : contents) {
            if (treeNode.getLabel().length() > imageWidth) {
                imageWidth = treeNode.getLabel().length() ;
            }
        }
        imageWidth = (imageWidth + 2) * 12;
        imageHeight = y;
    }

    private TreeNode getBranch(TreeNode fromBranch, List<String> components) {
        String branchName = components.getFirst();
        TreeNode branch = fromBranch.getChild(branchName);
        if (branch == null) {
            branch = new TreeNode(SvgIcon.folder(), branchName);
            fromBranch.addChild(branch);
            branch.setX(fromBranch.getX() + indent);
            branch.setY(y);
            branch.setParent(fromBranch);
            contents.add(branch);
            y += lineHeight;
        }
        if (components.size() > 1) {
            components.removeFirst();
            return getBranch(branch, components);
        }
        return branch;
    } 
}
