package io.github.sandydunlop.markista.model;

public class Reference {
    private Kind kind = Kind.NONE;
    private Scope scope = Scope.NONE;
    private String displayName = "";
    private String className = "";
    private String uri = "";
    private String anchor = "";

    /// Default constructor creates an empty reference with kind and scope set to NONE.
    public Reference() {
    }

    /// Constructs a Reference with given kind, name, and URI. Scope defaults to LOCAL.
    /// @param kind The kind of the reference.
    /// @param name The display name for the reference.
    /// @param uri The URI tied to the reference.
    public Reference(Kind kind, String name, String uri) {
        this(Scope.LOCAL, kind, name, uri);
    }

    /// Constructs a Reference with given scope, kind, and name. URI defaults to empty string.
    /// @param scope The scope of the reference.
    /// @param kind The kind/type of the reference.
    /// @param name The display name of the reference.
    public Reference(Scope scope, Kind kind, String name) {
        this(scope, kind, name, "");
    }

    /// Constructs a Reference with full specification: scope, kind, name, and URI.
    /// @param scope The scope of the reference.
    /// @param kind The kind/type of the reference.
    /// @param name The display name.
    /// @param uri The URI associated with the reference.
    public Reference(Scope scope, Kind kind, String name, String uri) {
        this.scope = scope;
        this.kind = kind;
        this.uri = uri;
        this.displayName = name;
    }

    /// Sets the kind/type of the reference.
    /// @param kind The kind to set.
    public void setKind(Kind kind) {
        this.kind = kind;
    }

    /// Returns the kind/type of the reference.
    /// @return The reference kind.
    public Kind getKind() {
        return kind;
    }

    /// Sets the scope of the reference.
    /// @param scope The scope to set.
    public void setScope(Scope scope) {
        this.scope = scope;
    }

    /// Returns the scope of the reference.
    /// @return The reference scope.
    public Scope getScope() {
        return scope;
    }

    /// Sets the display name of the reference.
    /// @param name The display name to set.
    public void setDisplayName(String name) {
        this.displayName = name;
    }

    /// Returns the display name of the reference.
    /// @return The display name.
    public String getDisplayName() {
        return displayName;
    }

    /// Sets the class name associated with the reference.
    /// @param name The class name to set.
    public void setClassName(String name) {
        this.className = name;
    }

    /// Returns the class name associated with the reference.
    /// @return The class name.
    public String getClassName() {
        return className;
    }

    /// Sets the URI of the reference.
    /// @param uri The URI to set.
    public void setUri(String uri) {
        this.uri = uri;
    }

    /// Returns the URI of the reference.
    /// @return The URI string.
    public String getUri() {
        return uri;
    }

    /// Sets the anchor part of the URI.
    /// @param anchor The anchor to set.
    public void setAnchor(String anchor) {
        this.anchor = anchor;
    }

    /// Returns the anchor part of the URI.
    /// @return The anchor string.
    public String getAnchor() {
        return anchor;
    }

    /// Enum representing different kinds/types of references.
    public enum Kind {
        NONE,
        UNKNOWN,
        URL,
        PAGE,
        MODULE,
        PACKAGE,
        TYPE,
        METHOD,
        PRIMITIVE,
        VOID
    }

    /// Enum representing the scope of references.
    public enum Scope {
        NONE,
        UNKNOWN,
        LOCAL,
        NATIVE,
        FOREIGN
    }
}
