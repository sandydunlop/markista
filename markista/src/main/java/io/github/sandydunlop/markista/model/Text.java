package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

public class Text {
    /// List of text segments composing this Text instance.
    private List<Segment> segments = new ArrayList<>();

    /// Creates an empty Text instance.
    /// @return A new empty Text object.
    public static Text empty() {
        return new Text();
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
    /// @return true if there are no segments or the segment list is null.
    public boolean isEmpty() {
        return segments == null || segments.isEmpty();
    }

    /// Returns the list of segments composing this Text.
    /// @return List of Text.Segment objects.
    public List<Text.Segment> getSegments() {
        return segments;
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
        segments.add(segment);
        return this;
    }

    /// Appends a single text segment to this Text.
    /// @param text text to go in the segment
    /// @return This Text instance for chaining.
    public Text append(String text) {
        segments.add(Segment.empty().setKind(SegmentKind.TEXT).setText(text));
        return this;
    }

    /// Appends all segments from another Text instance.
    /// @param text The Text whose segments should be appended.
    /// @return This Text instance for chaining.
    public Text append(Text text) {
        segments.addAll(text.getSegments());
        return this;
    }

    /// Represents a segment of the Text with its kind, content, and optional link.
    public static class Segment {
        /// The kind/type of this segment.
        private SegmentKind kind = SegmentKind.NONE;
        /// The textual content of this segment.
        private String text = "";
        /// The associated link if the segment represents a link.
        private String link = "";

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
        /// @param k The SegmentKind value.
        /// @return This Segment instance for chaining.
        public Segment setKind(SegmentKind k) {
            kind = k;
            return this;
        }

        /// Returns the kind of this segment.
        /// @return The current SegmentKind.
        public SegmentKind getKind() {
            return kind;
        }

        /// Returns the string representation of this segment.
        /// If text is null or empty, returns the link value.
        /// @return The text or link of this segment.
        public String toString() {
            if (text == null || text.isEmpty()) {
                return link;
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
        public Segment setLink(String link) {
            this.link = link;
            return this;
        }

        /// Returns the link associated with this segment.
        /// @return The link string.
        public String getLink() {
            return link;
        }
    }

    /// Enum class that defines different kinds of segments for Text.
    public enum SegmentKind {
        NONE,
        MARKDOWN,
        TEXT,
        LINK,
        CODE,
        START,
        END;
    }
}
