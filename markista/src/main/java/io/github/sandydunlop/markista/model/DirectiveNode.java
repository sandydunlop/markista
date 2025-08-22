package io.github.sandydunlop.markista.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/// Represents a directive within a module declaration, such as requires, exports, or provides.
/// Tracks the kind of directive, its name, whether it is transitive, associated packages,
/// implementations, and related interface information.
public class DirectiveNode implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /// The kind of this directive (e.g. REQUIRES, EXPORTS).
    private final Kind kind;

    /// The name associated with this directive.
    private String name = "";

    private Link reference;

    /// Indicates whether this directive is transitive.
    private boolean transitive;

    /// List of package names associated with this directive.
    private final List<Link> packages = new ArrayList<>();

    /// List of implementation names associated with this directive.
    private final List<Link> implementations = new ArrayList<>();

    /// The interface name associated with this directive, if any.
    private Link interfaceReference;

    /// Constructs a DirectiveNode with the specified kind, name and transitive flag.
    /// @param kind the kind of directive.
    /// @param reference the name associated with the directive.
    /// @param transitive true if the directive is transitive; false otherwise.
    public DirectiveNode(Kind kind, Link reference, boolean transitive) {
        this.kind = kind;
        this.reference = reference;
        this.transitive = transitive;
    }

    /// Constructs a DirectiveNode with the specified kind and name.
    /// The transitive flag is not set and defaults to false.
    /// @param kind the kind of directive.
    /// @param reference the name associated with the directive.
    public DirectiveNode(Kind kind, Link reference) {
        this.kind = kind;
        this.reference = reference;
    }

    /// Returns the kind of this directive.
    /// @return the directive kind.
    public Kind getKind() {
        return kind;
    }

    /// Returns the name associated with this directive.
    /// @return the directive name.
    public String getName() {
        return name;
    }

    public Link getReference() {
        return reference;
    }

    /// Indicates if this directive is transitive.
    /// @return true if transitive; false otherwise.
    public boolean isTransitive() {
        return transitive;
    }

    /// Adds a package reference to this directive's package list.
    /// @param reference A reference to thepackage to add.
    public void addPackage(Link reference) {
        packages.add(reference);
    }

    /// Returns the list of package references associated with this directive.
    /// @return list of package names.
    public List<Link> getPackages() {
        return packages;
    }

    /// Adds the reference to an implementation associated with this directive.
    /// @param reference A reference to the implementation to add.
    public void addImplementation(Link reference) {
        implementations.add(reference);
    }

    /// Returns the list of implementations associated with this directive.
    /// @return list of implementation references.
    public List<Link> getImplementations() {
        return implementations;
    }

    /// Sets the interface name related to this directive.
    /// @param interfaceReference the interface name to set.
    public void setInterface(Link interfaceReference) {
        this.interfaceReference = interfaceReference;
    }

    public String toString() {
        return kind.toString();
    }

    /// Returns the interface name associated with this directive.
    /// @return the interface name, or an empty string if none set.
    public Link getInterface() {
        return interfaceReference;
    }

    /// Enum representing the various kinds of directives possible in a module declaration.
    public enum Kind {
        /// No directive or unspecified type.
        NONE,

        /// The 'requires' directive indicates dependencies on other modules.
        REQUIRES,

        /// The 'exports' directive specifies packages exported to other modules.
        EXPORTS,

        /// The 'opens' directive makes packages accessible at runtime via reflection.
        OPENS,

        /// The 'uses' directive declares a service used by the module.
        USES,

        /// The 'provides' directive declares the implementation(s) of a service.
        PROVIDES
    }
}