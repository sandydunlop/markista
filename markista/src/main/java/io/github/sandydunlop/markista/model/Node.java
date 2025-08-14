package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/// The base class for all types of nodes in the API model.
public class Node {
    /// A unique identifier
    protected UUID uuid = UUID.randomUUID();

    /// The deprecation status of the node
    private Deprecation deprecation = Deprecation.NONE;

    /// Text describing the deprecation state of the node
    private Text deprecationText = Text.empty();

    /// Text showing when this node was added to the API
    private Text since = Text.empty();

    /// The first sentence of the Javadoc for this node
    protected Text firstSentence = Text.empty();

    /// The body text of the Javadoc for this node
    private final Text body = Text.empty();

    /// The full text of the Javadoc for this node
    private final Text fullBody = Text.empty();

    /// A list of references specified in this node's Javadoc
    private List<Reference> references = new ArrayList<>();

    /// The default constructor
    protected Node() {
        // Only here for the Javadoc
    }

    public UUID getUUID() {
        return uuid;
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
}
