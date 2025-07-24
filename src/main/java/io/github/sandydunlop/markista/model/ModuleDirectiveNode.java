package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

public class ModuleDirectiveNode {
    private Kind kind;
    private String name;
    private boolean transitive;
    private List<String> packages = new ArrayList<>();

    public ModuleDirectiveNode(Kind kind, String name, boolean transitive) {
        this.kind = kind;
        this.name = name;
        this.transitive = transitive;
    }

    public ModuleDirectiveNode(Kind kind, String name) {
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

    public enum Kind {
        NONE,
        REQUIRES,
        EXPORTS,
        OPENS,
        USES,
        PROVIDES
    }
}
