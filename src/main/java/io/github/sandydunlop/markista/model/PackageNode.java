package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

public class PackageNode extends Node {
    public String fullDescription = "";
    public List<PackageNode> packages = new ArrayList<>();
    public List<ClassNode> classes = new ArrayList<>();
    public List<InterfaceNode> interfaces = new ArrayList<>();
    public List<EnumNode> enumClasses = new ArrayList<>();
    public List<ExceptionNode> exceptionClasses = new ArrayList<>();
    public List<AnnotationNode> annotationClasses = new ArrayList<>();

    public PackageNode(String packageName) {
        this.qualifiedName = packageName;
    }

    public List<PackageNode> getPackages() {
        return packages;
    }
}
