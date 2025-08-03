package io.github.sandydunlop.markista.model;

import java.util.List;

/// A TypeOwner is a [Node] that can be set as the *owner* of another type.
public interface TypeOwner {
    /// Adds a type node to this owner.
    /// @param typeNode the TypeNode to add
    void addType(TypeNode typeNode);

    /// Returns a list of all type nodes owned by this owner.
    /// @return list of TypeNode objects
    List<TypeNode> getTypes();

    /// Returns a list of classes owned by this owner.
    /// @return list of PackageMember objects representing classes
    List<PackageMember> getClasses();

    /// Returns a list of interfaces owned by this owner.
    /// @return list of PackageMember objects representing interfaces
    List<PackageMember> getInterfaces();

    /// Returns a list of enums owned by this owner.
    /// @return list of PackageMember objects representing enums
    List<PackageMember> getEnums();

    /// Returns a list of annotations owned by this owner.
    /// @return list of PackageMember objects representing annotations
    List<PackageMember> getAnnotations();

    /// Returns the simple name of this owner.
    /// @return name as a String
    String getName();
}
