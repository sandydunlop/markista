package io.github.sandydunlop.markista.model;

public class Reference {
    public Kind kind = Kind.NONE;
    public Scope scope = Scope.NONE;
    public String url = "";
    public String typeName = null;

    public enum Kind {
        NONE,
        TYPE,
        URL
    }

    public enum Scope {
        NONE,
        LOCAL,
        NATIVE,
        FOREIGN
    }
}
