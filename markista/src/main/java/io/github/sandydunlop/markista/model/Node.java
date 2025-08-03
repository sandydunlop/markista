package io.github.sandydunlop.markista.model;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// The base class for all types of nodes in the API model.
public class Node {
    /// The simple form of the node's name
    protected String simpleName = "";

    /// The canonical form of the node's name
    protected String qualifiedName = "";

    /// A [PackageNode] representing the package the node belongs to
    protected PackageNode packageNode = null;

    /// A list of the modifiers a node has
    private Set<Modifier> modifiers = new HashSet<>();

    /// The deprecation status of the node
    private Deprecation deprecation = Deprecation.NONE;

    /// Text describing the deprecation state of the node
    private Text deprecationText = Text.empty();

    /// Text showing when this node was added to the API
    private Text since = Text.empty();

    /// The first sentence of the Javadoc for this node
    protected Text firstSentence = Text.empty();

    /// The body text of the Javadoc for this node
    private Text body = Text.empty();

    /// The full text of the Javadoc for this node
    private Text fullBody = Text.empty();

    /// A list of references specified in this node's Javadoc
    private List<Reference> references = new ArrayList<>();

    /// The default constructor
    protected Node() {
        // Only here for the Javadoc
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

    /// Sets the deprecation status for this node.
    /// @param deprecation The deprecation enum value.
    public void setDeprecation(Deprecation deprecation) {
        this.deprecation = deprecation;
    }

    /// Retrieves the deprecation status of this node.
    /// @return The deprecation enum value.
    public Deprecation getDeprecation() {
        return deprecation;
    }

    /// Sets the deprecation text.
    /// @param text The text describing the deprecation.
    public void setDeprecationText(Text text) {
        deprecationText = text;
    }

    /// Returns the deprecation text.
    /// @return The deprecation descriptive text.
    public Text getDeprecationText() {
        return deprecationText;
    }

    /// Sets the 'since' documentation text.
    /// @param text The since text.
    public void setSince(Text text) {
        since = text;
    }

    /// Returns the 'since' documentation text.
    /// @return The since text.
    public Text getSince() {
        return since;
    }

    /// Sets the first sentence of the documentation.
    /// @param text The first sentence text.
    public void setFirstSentence(Text text) {
        firstSentence.set(text);
    }

    /// Returns the first sentence of the documentation.
    /// @return The first sentence text.
    public Text getFirstSentence() {
        return firstSentence;
    }

    /// Sets the main body documentation text.
    /// @param text The body text.
    public void setBody(Text text) {
        body.set(text);
    }

    /// Returns the main body documentation text.
    /// @return The body text.
    public Text getBody() {
        return body;
    }

    /// Sets the full body documentation text including tags.
    /// @param text The full body text.
    public void setFullBody(Text text) {
        fullBody.set(text);
    }

    /// Returns the full body documentation text including tags.
    /// @return The full body text.
    public Text getFullBody() {
        return fullBody;
    }

    /// Sets the list of references for this node.
    /// @param refs List of Reference objects.
    public void setReferences(List<Reference> refs) {
        references = refs;
    }

    /// Returns the list of references associated with this node.
    /// @return List of Reference objects.
    public List<Reference> getReferences() {
        return references;
    }

    /// Returns a string representation of modifiers, excluding 'public'.
    /// The modifiers are sorted according to a predefined order.
    /// @return A string containing sorted modifiers separated by spaces.
    public String getModifiersString() {
        StringBuilder mods = new StringBuilder();
        List<Modifier> modifierList = ModifierSorter.sortModifiers(modifiers);
        for (Modifier mod : modifierList) {
            if (mod != Modifier.PUBLIC) {
                mods.append(mod.toString()).append(" ");
            }
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
