package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

/// Represents a module node that contains directives, packages, and constant values. 
/// Implements the PackageOwner interface to manage contained packages. 
public class ModuleNode extends Node implements PackageOwner{
    private List<DirectiveNode> directives = new ArrayList<>();
    private List<PackageMember> packages = new ArrayList<>();
    private List<FieldNode> constantValues = new ArrayList<>();

    /// Constructs a ModuleNode with the given module name.
    /// @param moduleName The qualified name of the module.
    public ModuleNode(String moduleName) {
        this.qualifiedName = moduleName;
    }

    /// Returns the name (qualified name) of this module.
    /// @return The module name.
    public String getName() {
        return qualifiedName;
    }

    /// Returns the list of packages contained in this module.
    /// @return List of PackageMember objects.
    @Override
    public List<PackageMember> getPackages() {
        return packages;
    }

    /// Adds a package to this module.
    /// @param packageNode The PackageNode to add.
    @Override
    public void addPackage(PackageNode packageNode) {
        packages.add(packageNode);
    }

    /// Adds a directive to this module.
    /// @param directive The DirectiveNode to add.
    public void addDirective(DirectiveNode directive) {
        directives.add(directive);
    }

    /// Returns the list of directives declared in this module.
    /// @return List of DirectiveNode objects.
    public List<DirectiveNode> getDirectives() {
        return directives;
    }

    /// Adds a constant field value to this module.
    /// @param constant The FieldNode representing the constant.
    public void addConstantValue(FieldNode constant) {
        constantValues.add(constant);
    }

    /// Returns the list of constant values defined in this module.
    /// @return List of FieldNode objects.
    public List<FieldNode> getConstantValues() {
        return constantValues;
    }

    /// Returns the list of 'exports' directives in this module.
    /// @return List of DirectiveNode objects filtered by EXPORTS kind.
    public List<DirectiveNode> getExports() {
        return directives.stream()
                        .filter(item -> item.getKind() == DirectiveNode.Kind.EXPORTS)
                        .toList();
    }

    /// Returns the list of 'requires' directives in this module.
    /// @return List of DirectiveNode objects filtered by REQUIRES kind.
    public List<DirectiveNode> getRequires() {
        return directives.stream()
                        .filter(item -> item.getKind() == DirectiveNode.Kind.REQUIRES)
                        .toList();
    }

    /// Returns the list of 'opens' directives in this module.
    /// @return List of DirectiveNode objects filtered by OPENS kind.
    public List<DirectiveNode> getOpens() {
        return directives.stream()
                        .filter(item -> item.getKind() == DirectiveNode.Kind.OPENS)
                        .toList();
    }

    /// Returns the list of 'uses' directives in this module.
    /// @return List of DirectiveNode objects filtered by USES kind.
    public List<DirectiveNode> getUses() {
        return directives.stream()
                        .filter(item -> item.getKind() == DirectiveNode.Kind.USES)
                        .toList();
    }

    /// Returns the list of 'provides' directives in this module.
    /// @return List of DirectiveNode objects filtered by PROVIDES kind.
    public List<DirectiveNode> getProvides() {
        return directives.stream()
                        .filter(item -> item.getKind() == DirectiveNode.Kind.PROVIDES)
                        .toList();
    }
}
