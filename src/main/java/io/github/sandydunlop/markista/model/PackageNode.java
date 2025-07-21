package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

public class PackageNode extends AbstractTypeOwner implements PackageMember {
    private List<PackageMember> packages = new ArrayList<>();

    public PackageNode(String packageName) {
        this.qualifiedName = packageName;
    }

    public List<PackageMember> getPackages() {
        return packages;
    }

    public void addPackage(PackageNode packageNode) {
        packages.add(packageNode);
    }

    @Override
    public String getName() {
        return qualifiedName;
    }

    @Override
    public Text getDescription() {
        return firstSentence;
    }
}
