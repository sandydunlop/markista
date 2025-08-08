package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/// An abstract class with a set of methods useful to other subclasses of [Node]
/// that can be owners of types.
public abstract class AbstractTypeOwner extends Node implements TypeOwner {

    /// A list of types owned by this type owner
    protected List<TypeNode> types = new ArrayList<>();

    /// Default constructor
    protected AbstractTypeOwner() {
        // Only here for the comments
    }

    /// Adds a type to the list of types *owned* by this instance.
    /// @param typeNode the type to add
    @Override
    public void addType(TypeNode typeNode) {
        types.add(typeNode);
    }

    /// Gets the list of types *owned* by this instance.
    public List<TypeNode> getTypes() {
        return types;
    }

    /// Gets the list of classes *owned* by this instance.
    public List<PackageMember> getClasses() {
        return types.stream()
                             .filter(item -> item.getKind() == TypeNode.Kind.CLASS)
                             .collect(Collectors.toList());
    }

    /// Gets the list of interfaces *owned* by this instance.
    public List<PackageMember> getInterfaces() {
        return types.stream()
                             .filter(item -> item.getKind() == TypeNode.Kind.INTERFACE)
                             .collect(Collectors.toList());
    }

    /// Gets the list of enums *owned* by this instance.
    public List<PackageMember> getEnums() {
        return types.stream()
                             .filter(item -> item.getKind() == TypeNode.Kind.ENUM)
                             .collect(Collectors.toList());
    }
    /// Gets the list of annotations *owned* by this instance.
    public List<PackageMember> getAnnotations() {
        return types.stream()
                             .filter(item -> item.getKind() == TypeNode.Kind.ANNOTATION)
                             .collect(Collectors.toList());
    }

    /// Adds a class to the list of classes *owned* by this instance.
    /// @param node the class to add
    public void addClass(ClassTypeNode node) {
        types.add(node);
    }

    /// Adds a interface to the list of interfaces *owned* by this instance.
    /// @param node the interface to add
    public void addInterface(InterfaceTypeNode node) {
        types.add(node);
    }

    /// Adds a enum to the list of enums *owned* by this instance.
    /// @param node the enum to add
    public void addEnum(EnumTypeNode node) {
        types.add(node);
    }

    /// Adds a annotation to the list of annotations *owned* by this instance.
    /// @param node the annotation to add
    public void addAnnotation(AnnotationTypeNode node) {
        types.add(node);
    }

    /// Sorts the nodes owned by this instance into alphabetical order.
    public void sort() {
        types.sort((o1, o2) -> o1.simpleName.compareTo(o2.simpleName));
    }
}
