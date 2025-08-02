package io.github.sandydunlop.markista.model;

/// Enum representing the deprecation status of an element
public enum Deprecation {
    // Indicates no deprecation.
    NONE,

    /// Indicates deprecated and marked for removal. 
    FOR_REMOVAL,

    /// Indicates deprecated but not necessarily for removal. 
    DEPRECATED
}

