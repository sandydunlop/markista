package io.github.sandydunlop.markista.model;

/// Represents a member belonging to a package, such as a class, interface, or sub-package. 
public interface PackageMember {
    /// Returns the simple name of this package member.
    /// @return The name as a String.
    String getName();

    /// Returns the description of this package member as a Text object.
    /// @return The description text.
    Text getDescription();
}
