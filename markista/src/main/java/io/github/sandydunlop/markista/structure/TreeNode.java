package io.github.sandydunlop.markista.structure;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TreeNode {
    NodeKind kind;
    Icon icon;
    String label;
    int x;
    int y;
    List<TreeNode> children = new ArrayList<>();
    TreeNode parent;
    Path path;
    boolean visible = true;

    public TreeNode(NodeKind kind, String label) {
        this.kind = kind;
        this.label = label;
        this.icon = new Icon(kind);
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getX() {
        return x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getY() {
        return y;
    }

    public String getLabel() {
        return label;
    }

    public Icon getIcon() {
        return icon;
    }

    public void addChild(TreeNode entry) {
        children.add(entry);
    }

    public TreeNode getParent() {
        return parent;
    }

    public void setParent(TreeNode parent) {
        this.parent = parent;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    public void setVisible(boolean b) {
        visible = b;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setKind(NodeKind kind) {
        this.kind = kind;
    }

    public NodeKind getKind() {
        return kind;
    }

    public TreeNode getChild(String name) {
        for (TreeNode child : children) {
            if (child.getLabel().equals(name)) {
                return child;
            }
        }
        return null;
    }
}
