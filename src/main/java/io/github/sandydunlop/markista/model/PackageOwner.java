package io.github.sandydunlop.markista.model;

import java.util.List;

public interface PackageOwner {

    List<PackageMember> getPackages();

    void addPackage(PackageNode packageNode);

}