package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

public class ModuleNode extends Node implements PackageOwner{
    private List<ModuleDirectiveNode> directives = new ArrayList<>();
    private List<PackageMember> packages = new ArrayList<>();
    private List<FieldNode> constantValues = new ArrayList<>();

    public ModuleNode(String moduleName) {
        this.qualifiedName = moduleName;
    }

    public String getName() {
        return qualifiedName;
    }

    @Override
    public List<PackageMember> getPackages() {
        return packages;
    }

    @Override
    public void addPackage(PackageNode packageNode) {
        packages.add(packageNode);
    }

    public void addDirective(ModuleDirectiveNode directive) {
        directives.add(directive);
    }

    public List<ModuleDirectiveNode> getDirectives() {
        return directives;
    }

    public void addConstantValue(FieldNode constant) {
        constantValues.add(constant);
    }

    public List<FieldNode> getConstantValues() {
        return constantValues;
    }
}
