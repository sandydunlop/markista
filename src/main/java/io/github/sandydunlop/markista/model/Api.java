package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import javax.lang.model.element.TypeElement;

public class Api extends AbstractTypeOwner {
    private List<PackageNode> packages = new ArrayList<>();
    private List<FieldNode> constantValues = new ArrayList<>();

    public Api() {
        // Nothing to see here
    }

    public List<PackageNode> getPackages() {
        return packages;
    }

    public List<FieldNode> getConstantValues() {
        return constantValues;
    }

    public void addPackage(PackageNode node) {
        packages.add(node);
    }

    public PackageNode getPackageNode(String qualifiedName) {
        for (PackageNode packageDoc : packages) {
            if (packageDoc.qualifiedName.equals(qualifiedName)){
                return packageDoc;
            }
        }
        return null;
    }

    public ClassNode getClassNode(TypeElement type) {
        if (type == null) return null;
        return (ClassNode)getTypeNode(type.getQualifiedName().toString());
    }

    public TypeNode getTypeNode(String qualifiedName) {
        if (types == null) return null;
        for (TypeNode typeNode : types) {
            if (typeNode.qualifiedName.equals(qualifiedName)){
                return typeNode;
            }
        }
        return null;
    }

    public TypeNode getTypeNode(TypeElement type) {
        return getTypeNode(type.getQualifiedName().toString());
    }

    public void sort() {
        Collections.sort(packages, (o1, o2) -> o2.qualifiedName.compareTo(o1.qualifiedName) );
        for (TypeNode node : getTypes()) {
            node.sort();
        }
    }

    public String getName() {
        return null;
    }
}
