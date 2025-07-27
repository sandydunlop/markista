package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleNode extends Node implements PackageOwner{
    private List<DirectiveNode> directives = new ArrayList<>();
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

    public void addDirective(DirectiveNode directive) {
        directives.add(directive);
    }

    public List<DirectiveNode> getDirectives() {
        return directives;
    }

    public void addConstantValue(FieldNode constant) {
        constantValues.add(constant);
    }

    public List<FieldNode> getConstantValues() {
        return constantValues;
    }

    public List<DirectiveNode> getExports() {
        return directives.stream()
                             .filter(item -> item.getKind() == DirectiveNode.Kind.EXPORTS)
                             .collect(Collectors.toList());
    }

    public List<DirectiveNode> getRequires() {
        return directives.stream()
                             .filter(item -> item.getKind() == DirectiveNode.Kind.REQUIRES)
                             .collect(Collectors.toList());
    }

    public List<DirectiveNode> getOpens() {
        return directives.stream()
                             .filter(item -> item.getKind() == DirectiveNode.Kind.OPENS)
                             .collect(Collectors.toList());
    }

    public List<DirectiveNode> getUses() {
        return directives.stream()
                             .filter(item -> item.getKind() == DirectiveNode.Kind.USES)
                             .collect(Collectors.toList());
    }

    public List<DirectiveNode> getProvides() {
        return directives.stream()
                             .filter(item -> item.getKind() == DirectiveNode.Kind.PROVIDES)
                             .collect(Collectors.toList());
    }
}
