package io.github.sandydunlop.markista.model;

import java.io.Serial;
import java.io.Serializable;

/// `target` encapsulates links to web pages, markdown pages, modules, packages, types, and methods.
public class Link implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /// The package the link is coming from. Empty string means there is no package. Null means it hasn't been set yet.
    private String originPackage = "";
    private String originType = "";
    private String target = "";

    private Kind kind = Kind.UNKNOWN;
    private Scope scope = Scope.UNKNOWN;
    private String moduleName = "";
    private String packageName = "";

    private String className = "";
    private String qualifiedClassName = "";
    private String simpleClassName = "";
    private String nestedClassName = "";

    private String label = "";
    private String uri = "";
    private boolean resolved = false;

    private String methodSignature = "";
    private String methodName = "";
    private String anchor = "";

    /// Default constructor creates an empty target with kind and scope set to NONE.
    public Link() {
    }

    /// Constructs a target with given kind, name, and URI. Scope defaults to LOCAL.
    /// @param kind The kind of the target.
    /// @param label The display name for the target.
    /// @param target What the refrence links to
    public Link(Kind kind, String label, String target) {
        this.scope = Scope.LOCAL;
        this.kind = kind;
        this.target = target;
        this.label = label;
    }

    /// Sets the target of the target.
    /// @param target The target to set.
    /// @return the target with target set
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

    /// Sets the origin of this target. This is the location that is being linked from.
    /// @param name The origin being linked from.
    public void setOriginPackage(String name) {
        this.originPackage = name;
    }

    /// Gets the origin of this target. This is the location that is being linked from.
    /// @return the origin
    public String getOriginPackage() {
        return originPackage;
    }

    public void setOriginType(String name) {
        originType = name;
    }

    public String getOriginType() {
        return originType;
    }

    /// Sets the target of this target. This is the location that is being linked to.
    /// @param target The target being linked to.
    public void setTarget(String target) {
        this.target = target;
    }

    /// Gets the target of this target. This is the location that is being linked from.
    /// @return the target
    public String getTarget() {
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

    public Link setModuleName(String name) {
        moduleName = name;
        return this;
    }

    public String getModuleName() {
        return moduleName;
    }

    public Link setPackageName(String name) {
        packageName = name;
        return this;
    }

    public String getPackageName() {
        return packageName;
    }

    /// Sets the class name associated with the target.
    /// @param name The class name to set.
    public Link setClassName(String name) {
        this.className = name;
        return this;
    }

    /// Returns the class name associated with the target.
    /// @return The class name.
    public String getClassName() {
        return className;
    }

    /// Sets the qualified class name associated with the target.
    /// @param name The qualified class name to set.
    public Link setQualifiedClassName(String name) {
        this.qualifiedClassName = name;
        return this;
    }

    /// Returns the qualified class name associated with the target.
    /// @return The qualified class name.
    public String getQualifiedClassName() {
        return qualifiedClassName;
    }

    /// Sets the simple class name associated with the target.
    /// @param name The simple class name to set.
    public Link setSimpleClassName(String name) {
        this.simpleClassName = name;
        return this;
    }

    /// Returns the simple class name associated with the target.
    /// @return The simple class name.
    public String getSimpleClassName() {
        return simpleClassName;
    }

    /// Sets the nested class name associated with the target.
    /// @param name The nested class name to set.
    public Link setNestedClassName(String name) {
        this.nestedClassName = name;
        return this;
    }

    /// Returns the nested class name associated with the target.
    /// @return The nested class name.
    public String getNestedClassName() {
        return nestedClassName;
    }




    /// Sets the display name of the target.
    /// @param label The display name to set.
    public void setLabel(String label) {
        this.label = label;
    }

    /// Returns the display name of the target.
    /// @return The display name.
    public String getLabel() {
        return label;
    }

    /// Sets the URI of the target.
    /// @param uri The URI to set.
    public void setUri(String uri) {
        this.uri = uri;
    }

    /// Returns the URI of the target.
    /// @return The URI string.
    public String getUri() {
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




    public void setMethodSignature(String signature) {
        methodSignature = signature;
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




    /// Sets the origin package of the target.
    /// @param name The origin package to set.
    /// @return the target with origin set
    public Link fromPackage(String name) {
        this.originPackage = name;
        return this;
    }

    public Link fromType(String name) {
        this.originType = name;
        return this;
    }

    /// Sets the display name of the target.
    /// @param label The display name to set.
    /// @return the target with label set
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
        return kind.toString() + " (" + target + ")";
    }

    /// Enum representing different kinds/types of targets.
    public enum Kind {
        /// Kind hasn't been set. Will possibly be resolved.
        UNKNOWN,

        /// The target is not recognized and won't be resolved
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
        FOREIGN
    }
}
