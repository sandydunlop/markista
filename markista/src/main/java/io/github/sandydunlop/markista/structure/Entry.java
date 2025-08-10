package io.github.sandydunlop.markista.structure;

import java.util.ArrayList;
import java.util.List;

public class Entry {
    SVGIcon icon;
    String label;
    int x;
    int y;
    List<Entry> children = new ArrayList<>();
    Entry parent;

    public Entry(SVGIcon icon, String label) {
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

    public void addEntry(Entry entry) {
        children.add(entry);
    }

    public Entry getParent() {
        return parent;
    }

    public void setParent(Entry parent) {
        this.parent = parent;
    }
}
