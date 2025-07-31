package io.github.sandydunlop.markista.model;

import java.util.ArrayList;
import java.util.List;

import io.github.sandydunlop.markista.util.Utils;

public class Text {
    private List<Segment> segments = new ArrayList<>();

    public static Text empty() {
        return new Text();
    }

    private Text() {
        // Nothing to see here
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Segment segment : segments) {
            sb.append(segment.toString());
        }
        return sb.toString();
    }

    public boolean isEmpty() {
        return segments == null || segments.isEmpty();
    }

    public List<Text.Segment> getSegments() {
        return segments;
    }

    public void set(Text text) {
        this.segments = text.getSegments();
    }

    public Text append(Segment segment) {
        segments.add(segment);
        return this;
    }

    public Text append(Text text) {
        segments.addAll(text.getSegments());
        return this;
    }

    public static class Segment {
        private SegmentKind kind = SegmentKind.NONE;
        private String text = "";
        private String link = "";

        private Segment() {
            // Nothing here
        }

        public static Segment empty() {
            return new Segment();
        }

        public Segment setKind(SegmentKind k) {
            kind = k;
            return this;
        }

        public SegmentKind getKind() {
            return kind;
        }

        public String toString() {
            if (Utils.isNullOrEmpty(text)) {
                return link;
            }
            return text;
        }

        public Segment setText(String text) {
            this.text = text;
            return this;
        }

        public String getText() {
            return text;
        }

        public Segment setLink(String link) {
            this.link = link;
            return this;
        }

        public String getLink() {
            return link;
        }
    }

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
