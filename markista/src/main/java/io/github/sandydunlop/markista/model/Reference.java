package io.github.sandydunlop.markista.model;

import java.io.Serial;
import java.io.Serializable;

/// `Reference` encapsulates links to web pages, markdown pages, modules, packages, types, and methods.
public class Reference implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Kind kind = Kind.UNKNOWN;
    private Scope scope = Scope.UNKNOWN;
    private String label = "";
    private String className = "";
    private String uri = "";
    private String anchor = "";
    private boolean resolved = false;
    private boolean hasAnchor = false;
    private String methodSignature = "";
    
    /// The package the link is coming from. Empty string means there is no package. Null means it hasn't been set yet.
    private String origin = null; 
    private String target = "";

    /// Default constructor creates an empty reference with kind and scope set to NONE.
    public Reference() {
    }

    /// Constructs a Reference with given kind, name, and URI. Scope defaults to LOCAL.
    /// @param kind The kind of the reference.
    /// @param label The display name for the reference.
    /// @param target What the refrence links to
    public Reference(Kind kind, String label, String target) {
        this.scope = Scope.LOCAL;
        this.kind = kind;
        this.target = target;
        this.label = label;
    }

    /// Sets the target of the reference.
    /// @param target The target to set.
    /// @return the reference with target set
    public static Reference to(String target) {
        Reference ref = new Reference();
        ref.setScope(Scope.UNKNOWN);
        ref.setTarget(target);
        return ref;
    }

    public static Reference toMethod(String signature) {
        Reference ref = new Reference();
        ref.setScope(Scope.UNKNOWN);
        ref.methodSignature = signature;
        return ref;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    /// Sets the origin package of the reference.
    /// @param origin The origin package to set.
    /// @return the reference with origin set
    public Reference from(String origin) {
        this.origin = origin;
        return this;
    }

    /// Sets the display name of the reference.
    /// @param label The display name to set.
    /// @return the reference with label set
    public Reference withLabel(String label) {
        this.label = label;
        return this;
    }

    public Reference withClassName(String className) {
        this.className = className;
        return this;
    }

    public Reference withUri(String uri) {
        this.uri = uri;
        return this;
    }

    /// Sets the kind/type of the reference.
    /// @param kind The kind to set.
    /// @return the label with kind set
    public Reference withKind(Kind kind) {
        this.kind = kind;
        return this;
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
    /// @param label The display name to set.
    public void setLabel(String label) {
        this.label = label;
    }

    /// Returns the display name of the reference.
    /// @return The display name.
    public String getLabel() {
        return label;
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

    /// Sets the origin of this reference. This is the location that is being linked from.
    /// @param origin The origin being linked from.
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    /// Gets the origin of this reference. This is the location that is being linked from.
    /// @return the origin
    public String getOrigin() {
        return origin;
    }

    /// Sets the target of this reference. This is the location that is being linked to.
    /// @param target The target being linked to.
    public void setTarget(String target) {
        this.target = target;
    }

    /// Gets the target of this reference. This is the location that is being linked from.
    /// @return the target
    public String getTarget() {
        return target;
    }

    /// Sets the resolved state of this reference.
    /// @param b Whether this reference is resolved or not.
    public void setResolved(boolean b) {
        resolved = b;
    }

    /// Gets the resolved status of this reference.
    /// @return True if this reference has been resolved. False otherwise.
    public boolean isResolved() {
        return resolved;
    }

    /// Specifies if this link is to an anchor
    /// @param b Whether this reference is to an anchor or not.
    public void setHasAnchor(boolean b) {
        hasAnchor = b;
    }

    /// Gets the anchor status of this reference.
    /// @return True if this reference is to an anchor. False otherwise.
    public boolean hasAnchor() {
        return hasAnchor;
    }

    public String toString() {
        return kind.toString() + " (" + target + ")";
    }

    /// Enum representing different kinds/types of references.
    public enum Kind {
        /// Kind hasn't been set. Will possibly be resolved.
        UNKNOWN,

        /// The reference is not recognized and won't be resolved
        UNSUPPORTED,

        /// A link to a webpage
        URL,

        /// A link to a Markdown page
        PAGE,

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

    /// Enum representing the scope of references.
    public enum Scope {
        /// Scope isn't known
        UNKNOWN,

        /// Within the module being documented
        LOCAL,

        /// Not in the module being documented, but we have the Javadoc locally
        SIBLING,

        /// Java built-in APIs
        NATIVE,

        /// Within an external project
        FOREIGN
    }
}
