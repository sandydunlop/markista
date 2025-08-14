package io.github.sandydunlop.markista.model;

import java.util.List;

/// Interface representing an owner of packages. public interface
public interface PackageOwnerInterface {

    /// Returns the list of packages owned by this owner.
    /// @return List of PackageNode objects representing owned packages.
    List<PackageNode> getPackages();

    /// Adds a package to the owned packages.
    /// @param packageNode The PackageNode to add.
    void addPackage(PackageNode packageNode);
    
}