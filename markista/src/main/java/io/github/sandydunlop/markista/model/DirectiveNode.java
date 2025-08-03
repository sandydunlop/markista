package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

/// Represents a directive within a module declaration, such as requires, exports, or provides.
/// Tracks the kind of directive, its name, whether it is transitive, associated packages,
/// implementations, and related interface information.
public class DirectiveNode {
    /// The kind of this directive (e.g. REQUIRES, EXPORTS).
    private Kind kind;

    /// The name associated with this directive.
    private String name;

    /// Indicates whether this directive is transitive.
    private boolean transitive;

    /// List of package names associated with this directive.
    private List<String> packages = new ArrayList<>();

    /// List of implementation names associated with this directive.
    private List<String> implementations = new ArrayList<>();

    /// The interface name associated with this directive, if any.
    private String interfaceName = "";

    /// Constructs a DirectiveNode with the specified kind, name and transitive flag.
    /// @param kind the kind of directive.
    /// @param name the name associated with the directive.
    /// @param transitive true if the directive is transitive; false otherwise.
    public DirectiveNode(Kind kind, String name, boolean transitive) {
        this.kind = kind;
        this.name = name;
        this.transitive = transitive;
    }

    /// Constructs a DirectiveNode with the specified kind and name.
    /// The transitive flag is not set and defaults to false.
    /// @param kind the kind of directive.
    /// @param name the name associated with the directive.
    public DirectiveNode(Kind kind, String name) {
        this.kind = kind;
        this.name = name;
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

    /// Indicates if this directive is transitive.
    /// @return true if transitive; false otherwise.
    public boolean isTransitive() {
        return transitive;
    }

    /// Adds a package name to this directive's package list.
    /// @param packageName the fully qualified package name to add.
    public void addPackage(String packageName) {
        packages.add(packageName);
    }

    /// Returns the list of package names associated with this directive.
    /// @return list of package names.
    public List<String> getPackages() {
        return packages;
    }

    /// Adds the name of an implementation associated with this directive.
    /// @param implementationName the name of the implementation to add.
    public void addImplementation(String implementationName) {
        implementations.add(implementationName);
    }

    /// Returns the list of implementations associated with this directive.
    /// @return list of implementation names.
    public List<String> getImplementations() {
        return implementations;
    }

    /// Sets the interface name related to this directive.
    /// @param interfaceName the interface name to set.
    public void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    /// Returns the interface name associated with this directive.
    /// @return the interface name, or an empty string if none set.
    public String getInterface() {
        return interfaceName;
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