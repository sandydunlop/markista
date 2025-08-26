package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

/// Represents a Java package
public class PackageNode extends Node {
    private String name;
    private String moduleName;
    private String sourcePath;
    private boolean hasPackageInfo = false;
    private final List<PackageNode> packages = new ArrayList<>();

    // store children by the interface type (no concrete TypeNode mention)
    protected final List<TypeNode> types = new ArrayList<>();

    /// Constructs a PackageNode with the specified qualified package name.
    /// @param name The qualified name of the package.
    public PackageNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
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

    public void addType(TypeNode typeNode) {
        types.add(typeNode);
    }

    /// Gets the list of types *owned* by this instance.
    public List<TypeNode> getTypes() {
        return List.copyOf(types);
    }

    /// Gets the list of classes *owned* by this instance.
    public List<TypeNode> getClasses() {
        List<TypeNode> out = new ArrayList<>();
        for (TypeNode t : types)
            if (t.isClass())
                out.add(t);
        return out;
    }

    /// Gets the list of interfaces *owned* by this instance.
    public List<TypeNode> getInterfaces() {
        List<TypeNode> out = new ArrayList<>();
        for (TypeNode t : types)
            if (t.isInterface())
                out.add(t);
        return out;
    }

    /// Gets the list of enums *owned* by this instance.
    public List<TypeNode> getEnums() {
        List<TypeNode> out = new ArrayList<>();
        for (TypeNode t : types)
            if (t.isEnum())
                out.add(t);
        return out;
    }

    /// Gets the list of records *owned* by this instance.
    public List<TypeNode> getRecords() {
        List<TypeNode> out = new ArrayList<>();
        for (TypeNode t : types)
            if (t.isRecord())
                out.add(t);
        return out;
    }

    /// Gets the list of annotations *owned* by this instance.
    public List<TypeNode> getAnnotations() {
        List<TypeNode> out = new ArrayList<>();
        for (TypeNode t : types)
            if (t.isAnnotation())
                out.add(t);
        return out;
    }

    /// Sorts the nodes owned by this instance into alphabetical order.
    public void sort() {
        types.sort((a, b) -> a.getSimpleName().compareTo(b.getSimpleName()));
    }
}
