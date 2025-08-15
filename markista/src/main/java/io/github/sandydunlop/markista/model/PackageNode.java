package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

/// Represents a Java package
public class PackageNode extends PackageOrTypeNode {
    private String moduleName;
    private String sourcePath;
    private boolean hasPackageInfo = false;
    private final List<PackageNode> packages = new ArrayList<>();

    /// Constructs a PackageNode with the specified qualified package name.
    /// @param name The qualified name of the package.
    public PackageNode(String name) {
        this.qualifiedName = name;
    }

    /// Sets the module for this package.
    /// @param module The ModuleNode that owns this package.
    public void setModuleName(String module) {
        this.moduleName = module;
    }

    /// Returns the module that owns this package.
    /// @return The ModuleNode instance.
    public String getModuleName() {
        return moduleName;
    }

    public void setSourcePath(String path) {
        this.sourcePath = path;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    /// Returns the list of package members owned by this package.
    /// @return List of PackageNode objects.
    public List<PackageNode> getPackages() {
        return packages;
    }

    /// Adds a subpackage to this package.
    /// @param packageNode The PackageNode to add as a member.
    public void addPackage(PackageNode packageNode) {
        packages.add(packageNode);
    }

    /// Returns the description text for this package, typically the first sentence.
    /// @return The Text object representing the description.
    public Text getDescription() {
        return firstSentence;
    }

    public void setHasPackageInfo(boolean b) {
        hasPackageInfo = b;
    }

    public boolean hasPackageInfo() {
        return hasPackageInfo;
    }
}
