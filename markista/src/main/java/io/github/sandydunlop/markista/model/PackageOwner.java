package io.github.sandydunlop.markista.model;

import java.util.List;

/// Interface representing an owner of packages. public interface
public interface PackageOwner {

    /// Returns the list of packages owned by this owner.
    /// @return List of PackageMember objects representing owned packages.
    List<PackageMember> getPackages();

    /// Adds a package to the owned packages.
    /// @param packageNode The PackageNode to add.
    void addPackage(PackageNode packageNode);
    
}