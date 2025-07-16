package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import javax.lang.model.element.TypeElement;

public class Api {
    private List<PackageNode> topLevelPackages = new ArrayList<>();
    private List<PackageNode> packages = new ArrayList<>();
    private List<ClassNode> classes = new ArrayList<>();
    private List<InterfaceNode> interfaces = new ArrayList<>();
    private List<EnumNode> enums = new ArrayList<>();
    private List<ExceptionNode> exceptions = new ArrayList<>();
    private List<AnnotationNode> annotations = new ArrayList<>();

    public Api() {
        // Nothing to see here
    }

    public List<PackageNode> getPackages() {
        return packages;
    }

    public List<ClassNode> getClasses() {
        return classes;
    }

    public List<InterfaceNode> getInterfaces() {
        return interfaces;
    }

    public List<EnumNode> getEnums() {
        return enums;
    }

    public List<ExceptionNode> getExceptions() {
        return exceptions;
    }

    public List <AnnotationNode> getAnnotations() {
        return annotations;
    }

    public void addPackage(PackageNode node) {
        packages.add(node);
    }

    public void addClass(ClassNode node) {
        classes.add(node);
    }

    public void addInterface(InterfaceNode node) {
        interfaces.add(node);
    }

    public void addEnum(EnumNode node) {
        enums.add(node);
    }

    public void addException(ExceptionNode node) {
        exceptions.add(node);
    }

    public void addAnnotation(AnnotationNode node) {
        annotations.add(node);
    }

    public PackageNode getPackageDoc(String qualifiedName) {
        for (PackageNode packageDoc : packages) {
            if (packageDoc.qualifiedName.equals(qualifiedName)){
                return packageDoc;
            }
        }
        return null;
    }

    public ClassNode getClassDoc(TypeElement type) {
        if (type == null) return null;
        return (ClassNode)getTypeDoc(type.getQualifiedName().toString(), classes);
    }

    //TODO: Make this efficient
    public TypeNode getTypeDoc(String qualifiedName, List<?> docs) {
        List<TypeNode> docList = (List<TypeNode>) docs;
        for (TypeNode doc : docList) {
            if (doc.qualifiedName.equals(qualifiedName)){
                return doc;
            }
        }
        return null;
    }

    public void sort() {
        Collections.sort(packages, (o1, o2) -> { return o2.qualifiedName.compareTo(o1.qualifiedName); });
        for (ClassNode node : classes) {
            node.sort();
        }
    }
}
