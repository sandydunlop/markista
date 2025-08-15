package io.github.sandydunlop.markista.model;

/**
 * Minimal view of a type used by PackageOrTypeNode.
 * Does not reference TypeNode to avoid cycles.
 */
public interface TypeView {
    String getQualifiedName();
    String getSimpleName();
    String getModifiersString();
    Text getFirstSentence();
    Text getDescription();
    String getKindName();
    void sort();

    // Query hooks to avoid referencing a Kind enum
    boolean isClass();
    boolean isInterface();
    boolean isEnum();
    boolean isAnnotation();
}