package io.github.sandydunlop.markista.model;

import java.io.Serial;
import java.io.Serializable;

public class TypeReference implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Reference reference;
    private Text text;

    private TypeReference() {
        // Nothing to see here
    }

    public static TypeReference to(String typeName) {
        TypeReference ref = new TypeReference();
        ref.setReference(Reference.to(typeName));
        ref.setText(Text.empty());
        return ref;
    }

    public static TypeReference to(Reference r, Text t) {
        TypeReference ref = new TypeReference();
        ref.setReference(r);
        ref.setText(t);
        return ref;
    }

    public void setReference(Reference ref) {
        reference = ref;
    }

    public Reference getReference() {
        return reference;
    }

    public void setText(Text text) {
        this.text = text;
    }

    public Text getText() {
        return text;
    }
}
