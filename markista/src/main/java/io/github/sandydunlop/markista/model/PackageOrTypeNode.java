package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

/// An abstract class with a set of methods useful to other subclasses of [Node]
/// that can be owners of types.
public abstract class PackageOrTypeNode extends AbstractPackageMember {
    /// A list of types owned by this type owner
    protected List<TypeNode> types = new ArrayList<>();

    /// Default constructor
    protected PackageOrTypeNode() {
        // Only here for the comments
    }

    /// Adds a type to the list of types *owned* by this instance.
    /// @param typeNode the type to add
    public void addType(TypeNode typeNode) {
        types.add(typeNode);
    }

    /// Gets the list of types *owned* by this instance.
    public List<TypeNode> getTypes() {
        return types;
    }

    /// Gets the list of classes *owned* by this instance.
    public List<TypeNode> getClasses() {
        return types.stream().filter(item -> item.getKind() == TypeNode.Kind.CLASS).toList();
    }

    /// Gets the list of interfaces *owned* by this instance.
    public List<TypeNode> getInterfaces() {
        return types.stream().filter(item -> item.getKind() == TypeNode.Kind.INTERFACE).toList();
    }

    /// Gets the list of enums *owned* by this instance.
    public List<TypeNode> getEnums() {
        return types.stream().filter(item -> item.getKind() == TypeNode.Kind.ENUM).toList();
    }
    /// Gets the list of annotations *owned* by this instance.
    public List<TypeNode> getAnnotations() {
        return types.stream().filter(item -> item.getKind() == TypeNode.Kind.ANNOTATION).toList();
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
