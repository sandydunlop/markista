package io.github.sandydunlop.markista.model;

import java.util.List;

public interface PackageOrTypeNode {
    public void addType(TypeView typeNode);

    /// Gets the list of types *owned* by this instance.
    public List<TypeView> getTypes();

    /// Gets the list of classes *owned* by this instance.
    public List<TypeView> getClasses() ;

    /// Gets the list of interfaces *owned* by this instance.
    public List<TypeView> getInterfaces();

    /// Gets the list of enums *owned* by this instance.
    public List<TypeView> getEnums();

    /// Gets the list of records *owned* by this instance.
    public List<TypeView> getRecords();

    /// Gets the list of annotations *owned* by this instance.
    public List<TypeView> getAnnotations();

    /// Sorts the nodes owned by this instance into alphabetical order.
    public void sort();
}
