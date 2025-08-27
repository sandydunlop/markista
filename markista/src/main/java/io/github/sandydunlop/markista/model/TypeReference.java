package io.github.sandydunlop.markista.model;

import java.io.Serial;
import java.io.Serializable;

public class TypeReference implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Link link;
    private Text text;
    private String qualifiedName;

    private TypeReference() {
        // Nothing to see here
    }

    public static TypeReference to(String typeName) {
        TypeReference ref = new TypeReference();
        ref.setLink(Link.to(typeName));
        ref.setText(Text.empty());
        ref.setQualifiedName(typeName);
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

    public void setQualifiedName(String name) {
        qualifiedName = name;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }
}
