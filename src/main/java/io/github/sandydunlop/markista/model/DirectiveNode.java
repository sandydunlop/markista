package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

public class DirectiveNode {
    private Kind kind;
    private String name;
    private boolean transitive;
    private List<String> packages = new ArrayList<>();
    private List<String> implementations = new ArrayList<>();
    private String interfaceName = "";

    public DirectiveNode(Kind kind, String name, boolean transitive) {
        this.kind = kind;
        this.name = name;
        this.transitive = transitive;
    }

    public DirectiveNode(Kind kind, String name) {
        this.kind = kind;
        this.name = name;
    }

    public Kind getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public boolean isTransitive() {
        return transitive;
    }

    public void addPackage(String packageName) {
        packages.add(packageName);
    }

    public List<String> getPackages() {
        return packages;
    }

    public void addImplementation(String implementationName) {
        implementations.add(implementationName);
    }

    public List<String> getImplementations() {
        return implementations;
    }

    public void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getInterface() {
        return interfaceName;
    }

    public enum Kind {
        NONE,
        REQUIRES,
        EXPORTS,
        OPENS,
        USES,
        PROVIDES
    }
}
