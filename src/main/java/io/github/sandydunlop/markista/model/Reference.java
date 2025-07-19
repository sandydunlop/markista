package io.github.sandydunlop.markista.model;

public class Reference {
    public Kind kind = Kind.NONE;
    public Scope scope = Scope.NONE;
    public String name = "";
    public String uri = "";

    public Reference() {
    }
    
    public Reference(Kind kind, String name, String value) {
        this(Scope.LOCAL, kind, name, value);
    }

    public Reference(Scope scope, Kind kind, String name, String uri) {
        this.scope = scope;
        this.kind = kind;
        this.uri = uri;
        this.name = name;
    }
    
    public enum Kind {
        NONE,
        PACKAGE,
        TYPE,
        PAGE,
        URL
    }

    public enum Scope {
        NONE,
        LOCAL,
        NATIVE,
        FOREIGN
    }
}
