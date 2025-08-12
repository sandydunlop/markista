package io.github.sandydunlop.markista.structure;

import java.util.List;

import io.github.sandydunlop.markista.model.ModuleNode;

public abstract class AbstractTree {
    ModuleNode module;

    TreeNode root;
    List<TreeNode> contents;

    int lineHeight = 18;
    int imageHeight = -1;
    int imageWidth = -1;
    int y = 0;
    int indent = 16;

    public void setModule(ModuleNode module) {
        this.module = module;
    }

    public TreeNode getRoot() {
        return root;
    }

    public List<TreeNode> getContents() {
        return contents;
    }

    public int getWidth() {
        return imageWidth;
    }

    public int getHeight() {
        return imageHeight;
    }

    public String getModuleName() {
        if (module == null) {
            return "unnamed module";
        }
        return module.getName();
    }

    public void makeAllVisible() {
        for (TreeNode node : contents) {
            node.setVisible(true);
        }
    }

    public void hideNames(String name) {
        for (TreeNode node : contents) {
            if (node.getLabel().contains(name)) {
                node.setVisible(false);
            }
        }
    }

    public void hideKind(NodeKind kind) {
        for (TreeNode node : contents) {
            if (node.getKind() == kind) {
                node.setVisible(false);
            }
        }
    }

    protected TreeNode getBranch(TreeNode fromBranch, List<String> components) {
        String branchName = components.getFirst();
        TreeNode branch = fromBranch.getChild(branchName);
        if (branch == null) {
            branch = new TreeNode(NodeKind.FOLDER, branchName);
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
