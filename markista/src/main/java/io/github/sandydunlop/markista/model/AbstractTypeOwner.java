package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractTypeOwner extends Node implements TypeOwner {
    protected List<TypeNode> types = new ArrayList<>();

    @Override
    public void addType(TypeNode typeNode) {
        types.add(typeNode);
    }

    public List<TypeNode> getTypes() {
        return types;
    }

    public List<PackageMember> getClasses() {
        return types.stream()
                             .filter(item -> item.getKind() == TypeNode.Kind.CLASS)
                             .collect(Collectors.toList());
    }

    public List<PackageMember> getInterfaces() {
        return types.stream()
                             .filter(item -> item.getKind() == TypeNode.Kind.INTERFACE)
                             .collect(Collectors.toList());
    }
    public List<PackageMember> getEnums() {
        return types.stream()
                             .filter(item -> item.getKind() == TypeNode.Kind.ENUM)
                             .collect(Collectors.toList());
    }
    public List<PackageMember> getAnnotations() {
        return types.stream()
                             .filter(item -> item.getKind() == TypeNode.Kind.ANNOTATION)
                             .collect(Collectors.toList());
    }

    public void addClass(ClassNode node) {
        types.add(node);
    }

    public void addInterface(InterfaceNode node) {
        types.add(node);
    }

    public void addEnum(EnumNode node) {
        types.add(node);
    }

    public void addException(ExceptionNode node) {
        types.add(node);
    }

    public void addAnnotation(AnnotationNode node) {
        types.add(node);
    }

    public void sort() {
        Collections.sort(types, (o1, o2) -> o1.simpleName.compareTo(o2.simpleName));
    }
}
