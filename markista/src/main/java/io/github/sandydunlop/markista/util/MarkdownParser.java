package io.github.sandydunlop.markista.util;

import java.util.ArrayList;
import java.util.List;

public class MarkdownParser {
    private int head = 0;
    private int tail = 0;
    int openBracket = -1;
    int openParenthesis = -1;
    int closeBracket = -1;
    int closeParenthesis = -1;
    private String markdown = "";
    private List<Segment> segments = new ArrayList<>();
    private Segment prev = null;

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

    private void processCharInGeneral() {
        // No special processing needed here for now.
    }

    private void handleOpenBracket() {
        saveText();
        openBracket = head;
        closeBracket = -1;
    }

    private boolean handleOpenParenthesis(char prevChar) {
        if (prevChar == ']') {
            openParenthesis = head;
            closeParenthesis = -1;
            return true;
        } else {
            return false;
        }
    }

    private void handleCloseBracket() {
        closeBracket = head;
        saveBracketsTag();
    }

    private boolean handleCloseParenthesis(boolean parensFollowBrackets) {
        closeParenthesis = head;
        if (parensFollowBrackets) {
            saveParensTag();
        }
        return false;
    }

    private void saveText() {
        Segment segment = new Segment(SegmentKind.TEXT);
        String text = markdown.substring(tail, head);
        if (text.isEmpty()) return;
        segment.setText(text);
        saveSegment(segment);
    }

    private void saveBracketsTag() {
        Segment segment = new Segment(SegmentKind.BRACKETS_TAG);
        String text = markdown.substring(openBracket + 1, closeBracket);
        segment.setText(text);
        saveSegment(segment);
    }

    private void saveParensTag() {
        if (openParenthesis == -1 || closeParenthesis == -1) {
            return;
        }
        Segment segment = new Segment(SegmentKind.PARENS_TAG);
        String text = markdown.substring(openParenthesis + 1, closeParenthesis);
        segment.setText(text);
        saveSegment(segment);
    }

    private void saveSegment(Segment segment) {
        segments.add(segment);
        if (prev != null) {
            prev.setNext(segment);
        }
        prev = segment;
    }

    public Segment firstSegment() {
        if (!segments.isEmpty()) {
            return segments.get(0);
        }
        return new Segment(SegmentKind.END);
    }

    public class Segment {
        Segment next = null;
        SegmentKind kind = SegmentKind.NONE;
        String text = "";

        public Segment(SegmentKind k) {
            kind = k;
        }

        public void setKind(SegmentKind k) {
            kind = k;
        }

        public SegmentKind getKind() {
            return kind;
        }

        public void setText(String t) {
            text = t;
        }

        public String getText() {
            return text;
        }

        public void setNext(Segment segment) {
            next = segment;
        }

        public Segment getNext() {
            if (next == null) {
                return new Segment(SegmentKind.END);
            }
            return next;
        }
    }

    public enum SegmentKind {
        NONE,
        TEXT,
        BRACKETS_TAG,
        PARENS_TAG,
        END
    }
}
