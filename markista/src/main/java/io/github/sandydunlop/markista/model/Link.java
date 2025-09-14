package io.github.sandydunlop.markista.model;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;

/// `target` encapsulates links to web pages, markdown pages, modules, packages, types, and methods.
public class Link implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private boolean resolved = false;
    protected Kind kind = Kind.UNRESOLVED;
    protected Scope scope = Scope.UNKNOWN;
    protected URI uri;

    protected Reference origin = null;
    protected Reference target = null;

    private String methodSignature = "";
    private String methodName = "";
    private String anchor = "";

    private SourceCodeLocation source = SourceCodeLocation.undefined();

    /// Default constructor creates an empty target with kind and scope set to NONE.
    public Link() {
        // Nothing to see here
    }

    /// Sets the target of the link to the provided URL.
    /// @param url The URL to use
    /// @return the link with its target set
    public static Link toWeb(URI url) {
        Link link = new Link();
        link.setUri(url);
        link.setResolved(true);
        link.setKind(Link.Kind.WEB);
        link.setScope(Scope.UNKNOWN);
        return link;
    }

    /// Sets the target of the link to the provided reference.
    /// @param ref The reference to use
    /// @return the link with its target set
    public static Link to(Reference ref) {
        Link link = new Link();
        link.target = ref;
        link.setScope(Scope.UNKNOWN);
        return link;
    }

    public Link from(Reference ref) {
        this.origin = ref;
        return this;
    }

    public void setSourceCodeLocation(SourceCodeLocation source) {
        this.source = source;
    }

    public SourceCodeLocation getSourceCodeLocation() {
        return source;
    }

    public void setOrigin(Reference ref) {
        origin = ref;
    }

    public Reference getOrigin() {
        return origin;
    }

    public Reference getTarget() {
        return target;
    }

    /// Sets the kind/type of the target.
    /// @param kind The kind to set.
    public void setKind(Kind kind) {
        this.kind = kind;
    }

    /// Returns the kind/type of the target.
    /// @return The target kind.
    public Kind getKind() {
        return kind;
    }

    /// Sets the scope of the target.
    /// @param scope The scope to set.
    public void setScope(Scope scope) {
        this.scope = scope;
    }

    /// Returns the scope of the target.
    /// @return The target scope.
    public Scope getScope() {
        return scope;
    }

    /// Sets the URI of the target.
    /// @param uri The URI to set.
    public void setUri(URI uri) {
        this.uri = uri;
    }

    /// Returns the URI of the target.
    /// @return The URI.
    public URI getUri() {
        return uri;
    }

    /// Sets the resolved state of this target.
    /// @param b Whether this target is resolved or not.
    public void setResolved(boolean b) {
        resolved = b;
    }

    /// Gets the resolved status of this target.
    /// @return True if this target has been resolved. False otherwise.
    public boolean isResolved() {
        return resolved;
    }

    public void setMethodSignature(String sig) {
        methodSignature = sig;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    /// Sets the method name of the target.
    /// @param name The method name to set.
    public Link setMethodName(String name) {
        this.methodName = name;
        return this;
    }

    /// Returns the method name of the target.
    /// @return The target method name.
    public String getMethodName() {
        return methodName;
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

    /// Sets the kind/type of the target.
    /// @param kind The kind to set.
    /// @return the label with kind set
    public Link withKind(Kind kind) {
        this.kind = kind;
        return this;
    }

    public Link withMethodName(String n) {
        methodName = n;
        return this;
    }




    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(kind.toString().toLowerCase());
        if (target != null) {
            Name name = target.getName();
            if (name != null) {
                if (name.isPackage()) {
                    sb.append(":package");
                } else if (name.isType()) {
                    sb.append(":type");
                } else if (name.isMember()) {
                    sb.append(":member");
                } else {
                    sb.append(":unknown");
                }
            } else {
                if (target.getModuleName() != null && !target.getModuleName().isEmpty()) {
                    sb.append(":module");
                } else {
                    sb.append(":unknown");
                }
            }
            sb.append(":");
            sb.append(target);
        }
        return sb.toString();
    }

    /// Enum representing different kinds/types of targets.
    public enum Kind {
        /// Kind hasn't been set. Will possibly be resolved.
        UNRESOLVED,

        /// The target is not recognized and won't be resolved
        UNSUPPORTED,

        /// A link to a webpage
        WEB,

        /// A link to a Markdown file
        FILE,

        /// A link to a Java module
        MODULE,

        /// A link to a Java package
        PACKAGE,

        /// A link to a Java type
        TYPE,

        /// A link to a JAva method
        METHOD,

        /// A Java primitive type
        PRIMITIVE,

        /// A link to Void
        VOID
    }

    /// Enum representing the scope of targets.
    public enum Scope {
        /// Scope isn't known
        UNKNOWN,

        /// Within the module being documented
        LOCAL,

        /// Not in the module being documented, but we have the Javadoc locally
        SIBLING,

        /// Java built-in APIs
        STANDARD,

        /// Within an external project
        EXTERNAL
    }
}
