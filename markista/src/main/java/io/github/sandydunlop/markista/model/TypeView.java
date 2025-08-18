package io.github.sandydunlop.markista.model;

/// Minimal view of a type used by PackageOrTypeNode.
/// Does not reference TypeNode to avoid cycles.
public interface TypeView {
    String getQualifiedName();
    String getSimpleName();
    String getModifiersString();
    String getKindName();
    Text getFirstSentence();
    void sort();

    // Query hooks to avoid referencing a Kind enum
    boolean isClass();
    boolean isInterface();
    boolean isEnum();
    boolean isAnnotation();
}