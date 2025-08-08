package io.github.sandydunlop.markista.util;

import java.util.ArrayList;
import java.util.List;

/// Parses a Markdown string into a sequence of segments representing text and special markup such as brackets and parentheses.
/// 
/// This parser is designed to tokenize the Markdown content by identifying textual parts and segments enclosed in brackets `[]` and parentheses `()`.
/// It also handles inline code spans denoted by backticks (`) to avoid parsing markup inside code.
/// 
/// The parsed segments can be iterated in sequence starting from the firstSegment() method.
/// Each segment carries its type (kind) which specifies how it should be interpreted or rendered.
/// 
/// Segment kinds include TEXT for normal text, BRACKETS_TAG for content inside square brackets,
/// PARENS_TAG for content inside parentheses following brackets (typical Markdown link syntax),
/// and END indicating the end of the sequence.
public class MarkdownParser {
    private int head = 0;
    private int tail = 0;
    int openBracket = -1;
    int openParenthesis = -1;
    int closeBracket = -1;
    int closeParenthesis = -1;
    private String markdown = "";
    private final List<Segment> segments = new ArrayList<>();
    private Segment prev = null;

    /// Creates a new MarkdownParser and immediately parses the provided Markdown string.
    /// @param md The Markdown string to parse.
    public MarkdownParser(String md) {
        if (Utils.isNullOrEmpty(md)) return;

        markdown = md;
        boolean inCode = false;
        char prevChar = ' ';
        boolean parensFollowBrackets = false;

        while (head < markdown.length()) {
            char c = markdown.charAt(head);
            if (c == '`') {
                inCode = !inCode;
                processCharInGeneral();
            } else if (!inCode) {
                switch (c) {
                    case '[' -> handleOpenBracket();
                    case '(' -> parensFollowBrackets = handleOpenParenthesis(prevChar);
                    case ']' -> {
                        handleCloseBracket();
                        tail = head + 1;
                    }
                    case ')' -> {
                        handleCloseParenthesis(parensFollowBrackets);
                        if (parensFollowBrackets) {
                            tail = head + 1;
                        }
                    }
                    default -> processCharInGeneral();
                }
                prevChar = c;
            }
            head++;
        }
        saveText();
    }

    /// Handles a character encountered when not in code or special markup.
    /// Currently a placeholder for any general character processing.
    private void processCharInGeneral() {
        // No special processing needed here for now.
    }

    /// Handles the open bracket '[' encountered in the markdown.
    /// Saves any preceding text segment before marking the position of the open bracket.
    private void handleOpenBracket() {
        saveText();
        openBracket = head;
        closeBracket = -1;
    }

    /// Handles an open parenthesis '(' encountered immediately after closing bracket ']'.
    /// @param prevChar The previous character before '(' to determine if parentheses follow brackets.
    /// @return true if parentheses follow brackets, false otherwise.
    private boolean handleOpenParenthesis(char prevChar) {
        if (prevChar == ']') {
            openParenthesis = head;
            closeParenthesis = -1;
            return true;
        } else {
            return false;
        }
    }

    /// Handles the close bracket ']' encountered in the markdown.
    /// Records the position and saves a bracket-tag segment.
    private void handleCloseBracket() {
        closeBracket = head;
        saveBracketsTag();
    }

    /// Handles the close parenthesis ')' encountered.
    /// If parentheses follow brackets, saves a parentheses-tag segment.
    /// @param parensFollowBrackets True if parentheses are following brackets (indicating a link).
    /// @return Always returns false to reset parsing state for parentheses.
    private boolean handleCloseParenthesis(boolean parensFollowBrackets) {
        closeParenthesis = head;
        if (parensFollowBrackets) {
            saveParensTag();
        }
        return false;
    }

    /// Saves any text from the tail position up to the current head as a TEXT segment.
    /// Does nothing if no text is available in that range.
    private void saveText() {
        Segment segment = new Segment(SegmentKind.TEXT);
        String text = markdown.substring(tail, head);
        if (text.isEmpty()) return;
        segment.setText(text);
        saveSegment(segment);
    }

    /// Saves the content between the most recent pair of brackets as a BRACKETS_TAG segment.
    private void saveBracketsTag() {
        Segment segment = new Segment(SegmentKind.BRACKETS_TAG);
        String text = markdown.substring(openBracket + 1, closeBracket);
        segment.setText(text);
        saveSegment(segment);
    }

    /// Saves the content between the most recent pair of parentheses as a PARENS_TAG segment.
    private void saveParensTag() {
        if (openParenthesis == -1 || closeParenthesis == -1) {
            return;
        }
        Segment segment = new Segment(SegmentKind.PARENS_TAG);
        String text = markdown.substring(openParenthesis + 1, closeParenthesis);
        segment.setText(text);
        saveSegment(segment);
    }

    /// Adds the specified segment to the list and links it to the previously saved segment.
    /// @param segment The Segment to save and link.
    private void saveSegment(Segment segment) {
        segments.add(segment);
        if (prev != null) {
            prev.setNext(segment);
        }
        prev = segment;
    }

    /// Returns the first Segment in the parsed sequence.
    /// If no segments exist, returns an END kind Segment.
    /// @return The first Segment or an END Segment if none exist.
    public Segment firstSegment() {
        if (!segments.isEmpty()) {
            return segments.getFirst();
        }
        return new Segment(SegmentKind.END);
    }

    /// Represents a segment of the parsed Markdown input.
    /// A segment has a kind and associated text content, and links to the next segment in sequence.
    public class Segment {
        Segment next = null;
        SegmentKind kind;
        String text = "";

        /// Creates a Segment with the specified kind.
        /// @param k The SegmentKind.
        public Segment(SegmentKind k) {
            kind = k;
        }

        /// Sets the Segment kind.
        /// @param k The SegmentKind to set.
        public void setKind(SegmentKind k) {
            kind = k;
        }

        /// Returns the Segment kind.
        /// @return The SegmentKind.
        public SegmentKind getKind() {
            return kind;
        }

        /// Sets the text content of the Segment.
        /// @param t The text string.
        public void setText(String t) {
            text = t;
        }

        /// Returns the text content of the Segment.
        /// @return The text string.
        public String getText() {
            return text;
        }

        /// Sets the next Segment in the sequence.
        /// @param segment The next Segment.
        public void setNext(Segment segment) {
            next = segment;
        }

        /// Returns the next Segment, or an END Segment if none exists.
        /// @return The next Segment or an END Segment.
        public Segment getNext() {
            if (next == null) {
                return new Segment(SegmentKind.END);
            }
            return next;
        }
    }

    /// Enum representing the kind of a Markdown segment.
    public enum SegmentKind {
        /// No specific kind assigned.
        NONE,

        /// Plain text segment.
        TEXT,

        /// Content inside square brackets.
        BRACKETS_TAG,

        /// Content inside parentheses following brackets.
        PARENS_TAG,

        /// End of sequence marker.
        END
    }
}