package io.github.sandydunlop.markista.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.structure.SVGIcon.Kind;

public class DirectoryTree extends SvgTreeWriter {
    public DirectoryTree() {
    }

    public void setModule(ModuleNode module) {
        this.module = module;
    }

    public TreeNode getRoot() {
        return root;
    }

    public List<TreeNode> getContents() {
        return contents;
    }

    public void build(TreeNode moduleRoot, List<TreeNode> moduleContents) {
        y = 0;
        root = new TreeNode(SVGIcon.folder(), moduleRoot.getLabel());
        contents = new ArrayList<>();
        contents.add(root);
        y += lineHeight;
        for (TreeNode node : moduleContents) {
            if (node.getIcon().getKind() == Kind.CODE) {
                if (node != moduleRoot) {
                    String packageName = node.getParent().getLabel();
                    List<String> list = new ArrayList<>(Arrays.asList(packageName.split("\\.")));
                    TreeNode branch = getBranch(root, list);
                    TreeNode leaf = new TreeNode(SVGIcon.code(), node.getLabel() + ".java");
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
            branch = new TreeNode(SVGIcon.folder(), branchName);
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
