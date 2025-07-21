package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

import com.sun.source.doctree.DocTree;

public class Text {
    private List<? extends DocTree> text = new ArrayList<>();

    public static Text empty() {
        return new Text();
    }

    private Text() {
        // Nothing to see here
    }

    public static Text fromDocTree(List<? extends DocTree> docTree) {
        return new Text(docTree);
    }

    public Text(List<? extends DocTree> t) {
        text = t;
    }

    public boolean isEmpty() {
        return text == null || text.isEmpty();
    }

    public void set(List<? extends DocTree> t) {
        text = t;
    }

    public List<? extends DocTree> getSegments() {
        return text;
    }
}
