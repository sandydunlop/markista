package io.github.sandydunlop.markista.structure;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TreeNode {
    SVGIcon icon;
    String label;
    int x;
    int y;
    List<TreeNode> children = new ArrayList<>();
    TreeNode parent;
    Path path;

    public TreeNode(SVGIcon icon, String label) {
        this.icon = icon;
        this.label = label;
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

    public SVGIcon getIcon() {
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

    // public List<TreeNode> getChildren() {
    //     return children;
    // }


    public TreeNode getChild(String name) {
        for (TreeNode child : children) {
            if (child.getLabel().equals(name)) {
                return child;
            }
        }
        return null;
    }
}
