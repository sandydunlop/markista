package io.github.sandydunlop.markista.model;

public class Reference {
    private Kind kind = Kind.NONE;
    private Scope scope = Scope.NONE;
    private String name = "";
    private String uri = "";

    public Reference() {
    }
    
    public Reference(Kind kind, String name, String uri) {
        this(Scope.LOCAL, kind, name, uri);
    }

    public Reference(Scope scope, Kind kind, String name, String uri) {
        this.scope = scope;
        this.kind = kind;
        this.uri = uri;
        this.name = name;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public Scope getScope() {
        return scope;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }
    
    public enum Kind {
        NONE,
        PACKAGE,
        TYPE,
        PAGE,
        URL,
        PRIMITIVE,
        VOID
    }

    public enum Scope {
        NONE,
        LOCAL,
        NATIVE,
        FOREIGN
    }
}
