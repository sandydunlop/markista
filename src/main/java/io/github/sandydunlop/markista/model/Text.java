package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

import com.sun.source.doctree.DocTree;

public class Text {
    public List<? extends DocTree> text = new ArrayList<>();

    public static Text empty() {
        return new Text();
    }

    private Text() {
        // Nothing to see here
    }

    public Text(List<? extends DocTree> t) {
        text = t;
    }

    public boolean isEmpty() {
        return text == null || text.size() == 0;
    }

    public void set(List<? extends DocTree> t) {
        text = t;
    }

}
