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
        return module.getName();
    }
}
