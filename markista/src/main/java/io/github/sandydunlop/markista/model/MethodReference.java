package io.github.sandydunlop.markista.model;

import java.io.Serial;
import java.io.Serializable;

public class MethodReference implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Link link;
    private Text text;

    private MethodReference() {
        // Nothing to see here
    }

    public static MethodReference to(String methodName) {
        MethodReference ref = new MethodReference();
        ref.setLink(Link.to(methodName));
        ref.setText(Text.empty());
        return ref;
    }

    public void setLink(Link ref) {
        link = ref;
    }

    public Link getLink() {
        return link;
    }

    public void setText(Text text) {
        this.text = text;
    }

    public Text getText() {
        return text;
    }
}
