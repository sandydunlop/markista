package io.github.sandydunlop.markista.model;

import java.io.Serial;
import java.io.Serializable;

/// `Reference` encapsulates links to web pages, markdown pages, modules, packages, types, and methods.
public class Link implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Kind kind = Kind.UNKNOWN;
    private Scope scope = Scope.UNKNOWN;
    private String label = "";
    private String packageName = "";
    private String simpleClassName = "";
    private String qualifiedClassName = "";
    private String uri = "";
    private String anchor = "";
    private boolean resolved = false;
    private String methodSignature = "";
    private String methodName = "";

    /// The package the link is coming from. Empty string means there is no package. Null means it hasn't been set yet.
    private String originPackage = "";
    private String originType = "";
    private String target = "";

    /// Default constructor creates an empty reference with kind and scope set to NONE.
    public Link() {
    }

    /// Constructs a Reference with given kind, name, and URI. Scope defaults to LOCAL.
    /// @param kind The kind of the reference.
    /// @param label The display name for the reference.
    /// @param target What the refrence links to
    public Link(Kind kind, String label, String target) {
        this.scope = Scope.LOCAL;
        this.kind = kind;
        this.target = target;
        this.label = label;
    }

    /// Sets the target of the reference.
    /// @param target The target to set.
    /// @return the reference with target set
    public static Link to(String target) {
        Link ref = new Link();
        ref.setScope(Scope.UNKNOWN);
        ref.setTarget(target);
        return ref;
    }

    public static Link toMethod(String signature) {
        Link ref = new Link();
        ref.setScope(Scope.UNKNOWN);
        ref.methodSignature = signature;
        return ref;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    /// Sets the origin package of the reference.
    /// @param name The origin package to set.
    /// @return the reference with origin set
    public Link fromPackage(String name) {
        this.originPackage = name;
        return this;
    }

    public Link fromType(String name) {
        this.originType = name;
        return this;
    }

    /// Sets the display name of the reference.
    /// @param label The display name to set.
    /// @return the reference with label set
    public Link withLabel(String label) {
        this.label = label;
        return this;
    }

    public Link withClassName(String className) {
        this.qualifiedClassName = className;
        return this;
    }

    public Link withUri(String uri) {
        this.uri = uri;
        return this;
    }

    /// Sets the kind/type of the reference.
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

    /// Sets the method name of the reference.
    /// @param name The method name to set.
    public Link setMethodName(String name) {
        this.methodName = name;
        return this;
    }

    /// Returns the method name of the reference.
    /// @return The reference method name.
    public String getMethodName() {
        return methodName;
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

    public Link setPackageName(String name) {
        packageName = name;
        return this;
    }

    public String getPackageName() {
        return packageName;
    }

    /// Sets the simple class name associated with the reference.
    /// @param name The simple class name to set.
    public Link setSimpleClassName(String name) {
        this.simpleClassName = name;
        return this;
    }

    /// Returns the simple class name associated with the reference.
    /// @return The simple class name.
    public String getSimpleClassName() {
        return simpleClassName;
    }

    /// Sets the qualified class name associated with the reference.
    /// @param name The qualified class name to set.
    public Link setQualifiedClassName(String name) {
        this.qualifiedClassName = name;
        return this;
    }

    /// Returns the qualified class name associated with the reference.
    /// @return The qualified class name.
    public String getQualifiedClassName() {
        return qualifiedClassName;
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
    public void setOriginPackage(String origin) {
        this.originPackage = origin;
    }

    /// Gets the origin of this reference. This is the location that is being linked from.
    /// @return the origin
    public String getOriginPackage() {
        return originPackage;
    }

    public String getOriginType() {
        return originType;
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

    public void setMethodSignature(String signature) {
        this.methodSignature = signature;
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
        STANDARD,

        /// Within an external project
        FOREIGN
    }
}
