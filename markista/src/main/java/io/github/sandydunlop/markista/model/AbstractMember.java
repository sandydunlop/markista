package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractMember extends Node {
    /// A list of the modifiers a node has
    private final Set<Modifier> modifiers = new HashSet<>();

    /// The simple form of the name
    protected String simpleName = "";

    /// The qualified form of the name
    protected String qualifiedName;

    /// A [PackageNode] representing the package the node belongs to
    protected String packageName = null;

    /// List of annotations applied to this type.
    private final List<AppliedAnnotationNode> appliedAnnotations = new ArrayList<>();

    /// Has the `@Documented` annotation applied
    private boolean hasDocumentedAnnotation = false;

    /// Sets the simple name of this type.
    /// @param name the simple name to set.
    public void setSimpleName(String name) {
        simpleName = name;
    }

    /// Returns the simple name of this type.
    /// @return the simple name.
    public String getSimpleName() {
        return simpleName;
    }

    /// Sets the qualified name of this type.
    /// @param name the qualified name to set.
    public void setQualifiedName(String name) {
        qualifiedName = name;
    }

    /// Returns the qualified name of this type.
    /// @return the qualified name.
    public String getQualifiedName() {
        return qualifiedName;
    }

    /// Adds a modifier to the set of modifiers.
    /// @param mod The modifier to add.
    public void addModifier(Modifier mod) {
        modifiers.add(mod);    
    }

    /// Returns the set of modifiers for this node.
    /// @return Set of modifiers.
    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    /// Adds an applied annotation to this type
    /// @param annotation the annotation
    public void addAppliedAnnotation(AppliedAnnotationNode annotation) {
        appliedAnnotations.add(annotation);
    }

    /// Returns the list of annotations applied to this type.
    /// @return list of applied annotations
    public List<AppliedAnnotationNode> getAppliedAnnotations() {
        return appliedAnnotations;
    }

    /// Sets a flag indicating if this type as having a `@Documented` meta-annotation
    /// @param b If true, this type is marked as having a `@Documented` meta-annotation.
    /// If false, it is marked as not having the met-annotation.
    public void setHasDocumentedAnnotation(boolean b) {
        this.hasDocumentedAnnotation = b;
    }

    /// Does this type have a `@Documented` meta-annotation?
    /// @return True if it has a `@Documented` meta-annotation
    public boolean hasDocumentedAnnotation() {
        return hasDocumentedAnnotation;
    }

    /// Returns a string representation of modifiers.
    /// The modifiers are sorted according to a predefined order.
    /// @return A string containing sorted modifiers separated by spaces.
    public String getModifiersString() {
        StringBuilder mods = new StringBuilder();
        List<Modifier> modifierList = ModifierSorter.sortModifiers(modifiers);
        for (Modifier mod : modifierList) {
            mods.append(mod.toString()).append(" ");
        }
        return mods.toString();
    }

    /// Utility class to sort modifiers according to Java language conventions.
    public static class ModifierSorter {
        /// Private constructor to prevent instantiation.
        private ModifierSorter() {
            // Hiding the public constructor
        }

        /// The fixed order of modifiers as per Java language specification.
        private static final List<Modifier> ORDER = Arrays.asList(
            Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE,
            Modifier.STATIC, Modifier.FINAL, Modifier.ABSTRACT,
            Modifier.SYNCHRONIZED, Modifier.TRANSIENT, Modifier.VOLATILE,
            Modifier.NATIVE, Modifier.STRICTFP, Modifier.DEFAULT
        );

        /// Sort the given set of modifiers into the standard order.
        /// @param modifierSet The set of modifiers to sort.
        /// @return List of modifiers sorted in the defined order.
        public static List<Modifier> sortModifiers(Set<Modifier> modifierSet) {
            List<Modifier> modifierList = new ArrayList<>(modifierSet);
            modifierList.sort(Comparator.comparingInt(ORDER::indexOf));
            return modifierList;
        }
    }
}
