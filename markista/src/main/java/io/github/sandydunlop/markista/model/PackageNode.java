package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

/// Represents a Java package
public class PackageNode extends AbstractTypeOwner implements PackageMember, PackageOwner {
    private ModuleNode module;
    private List<PackageMember> packages = new ArrayList<>();

    /// Constructs a PackageNode with the specified qualified package name.
    /// @param packageName The qualified name of the package.
    public PackageNode(String packageName) {
        this.qualifiedName = packageName;
    }

    /// Sets the module for this package.
    /// @param module The ModuleNode that owns this package.
    public void setModule(ModuleNode module) {
        this.module = module;
    }

    /// Returns the module that owns this package.
    /// @return The ModuleNode instance.
    public ModuleNode getModule() {
        return module;
    }

    /// Returns the list of package members owned by this package.
    /// @return List of PackageMember objects.
    @Override
    public List<PackageMember> getPackages() {
        return packages;
    }

    /// Adds a subpackage to this package.
    /// @param packageNode The PackageNode to add as a member.
    @Override
    public void addPackage(PackageNode packageNode) {
        packages.add(packageNode);
    }

    /// Returns the name (qualified name) of this package.
    /// @return The qualified package name.
    @Override
    public String getName() {
        return qualifiedName;
    }

    /// Returns the description text for this package, typically the first sentence.
    /// @return The Text object representing the description.
    @Override
    public Text getDescription() {
        return firstSentence;
    }
}
