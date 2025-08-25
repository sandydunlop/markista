package io.github.sandydunlop.markista.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/// Represents a block of text. Text stores the text as a list of segments which
/// can be of kind text, markdown, link, or code for easy retrieval and manipulation.
public class Text implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /// List of text segments composing this Text instance.
    private List<Segment> segments = new ArrayList<>();

    /// Creates an empty Text instance.
    /// @return A new empty Text object.
    public static Text empty() {
        return new Text();
    }

    /// Creates a Text object containing the supplied string value
    /// @param string a String to store in the Text object
    /// @return A Text object representing the string
    public static Text of(String string) {
        return new Text().append(string);
    }

    /// Creates a Text object containing the supplied segment value
    /// @param segment a Segment to store in the Text object
    /// @return A Text object representing the segment
    public static Text of(Segment segment) {
        return new Text().append(segment);
    }

    /// Private constructor to prevent external instantiation.
    private Text() {
        // Nothing to see here
    }

    /// Returns the combined string representation of all segments.
    /// @return Concatenated string of all segment texts.
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Segment segment : segments) {
            sb.append(segment.toString());
        }
        return sb.toString();
    }

    /// Checks whether this Text instance is empty.
    /// @return true if there are no segments.
    public boolean isEmpty() {
        return segments.isEmpty();
    }

    /// Returns the list of segments composing this Text.
    /// @return List of Text.Segment objects.
    public List<Text.Segment> getSegments() {
        return segments;
    }

    public Text.Segment getSegment(int n) {
        return segments.get(n);
    }

    /// Sets the segments of this Text to match another Text.
    /// @param text The Text source to copy from.
    public void set(Text text) {
        this.segments = text.getSegments();
    }

    /// Appends a single segment to this Text.
    /// @param segment The segment to add.
    /// @return This Text instance for chaining.
    public Text append(Segment segment) {
        if (!segment.getText().isEmpty() || 
                !segment.getLink().getTarget().isEmpty() ||
                segment.getKind() == Segment.Kind.INHERIT) {
            segments.add(segment);
        }
        return this;
    }

    /// Appends a single text segment to this Text.
    /// @param text text to go in the segment
    /// @return This Text instance for chaining.
    public Text append(String text) {
        if (!text.isEmpty()) {
            segments.add(Segment.empty().setKind(Segment.Kind.TEXT).setText(text));
        }
        return this;
    }

    /// Appends all segments from another Text instance.
    /// @param text The Text whose segments should be appended.
    /// @return This Text instance for chaining.
    public Text append(Text text) {
        segments.addAll(text.getSegments());
        return this;
    }

    public Text subtext(int start) {
        Text text = Text.empty();
        for (int i = start; i < segments.size(); i++) {
            text.append(segments.get(i));
        }
        return text;
    }

    public Text subtext(int start, int end) {
        Text text = Text.empty();
        for (int i = start; i <= end; i++) {
            text.append(segments.get(i));
        }
        return text;
    }

    /// Represents a segment of the Text with its kind, content, and optional link.
    public static class Segment implements Serializable{
        @Serial
        private static final long serialVersionUID = 1L;

        /// The kind/type of this segment.
        private Segment.Kind kind = Segment.Kind.NONE;
        /// The textual content of this segment.
        private String text = "";
        /// The associated link if the segment represents a link.
        private Link link = new Link();

        /// Private constructor to restrict instantiation.
        private Segment() {
            // Nothing here
        }

        /// Creates an empty segment instance.
        /// @return A new empty Segment object.
        public static Segment empty() {
            return new Segment();
        }

        /// Sets the kind of this segment.
        /// @param k The Segment.Kind value.
        /// @return This Segment instance for chaining.
        public Segment setKind(Segment.Kind k) {
            kind = k;
            return this;
        }

        /// Returns the kind of this segment.
        /// @return The current Segment.Kind.
        public Segment.Kind getKind() {
            return kind;
        }

        /// Returns the string representation of this segment.
        /// If text is null or empty, returns the link value.
        /// @return The text or link of this segment.
        public String toString() {
            if (text == null || text.isEmpty()) {
                return link.getLabel();
            }
            return text;
        }

        /// Sets the text content of this segment.
        /// @param text The text to set.
        /// @return This Segment instance for chaining.
        public Segment setText(String text) {
            this.text = text;
            return this;
        }

        /// Returns the text content of this segment.
        /// @return The text string.
        public String getText() {
            return text;
        }

        /// Sets the link value of this segment.
        /// @param link The link to set.
        /// @return This Segment instance for chaining.
        public Segment setLink(Link link) {
            this.link = link;
            return this;
        }

        /// Returns the link associated with this segment.
        /// @return The link reference.
        public Link getLink() {
            return link;
        }

        /// Enum class that defines different kinds of segments for Text.
        public enum Kind {
            /// Empty segment
            NONE,

            /// Plain test
            TEXT,

            /// A link
            LINK,

            /// Source code
            CODE,

            /// Inherited documentation
            INHERIT,
        }
    }
}
