package io.github.sandydunlop.markista.scanning;

import java.util.ArrayList;
import java.util.List;

/// Parses a Markdown string into a sequence of tokens representing text and special markup such as brackets and parentheses.
/// 
/// This parser is designed to tokenize the Markdown content by identifying textual parts and tokens enclosed in brackets `[]` and parentheses `()`.
/// It also handles inline code spans denoted by backticks (`) to avoid parsing markup inside code.
/// 
/// The parsed tokens can be iterated in sequence starting from the firstToken() method.
/// Each token carries its type (kind) which specifies how it should be interpreted or rendered.
/// 
/// Token kinds include TEXT for normal text, BRACKETS_TAG for content inside square brackets,
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
    private final List<Token> tokens = new ArrayList<>();
    private Token prev = null;

    /// Creates a new MarkdownParser and immediately parses the provided Markdown string.
    /// @param md The Markdown string to parse.
    public MarkdownParser(String md) {
        if (md == null || md.isEmpty()) return;

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
    /// Saves any preceding text token before marking the position of the open bracket.
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
    /// Records the position and saves a bracket-tag token.
    private void handleCloseBracket() {
        closeBracket = head;
        saveBracketsTag();
    }

    /// Handles the close parenthesis ')' encountered.
    /// If parentheses follow brackets, saves a parentheses-tag token.
    /// @param parensFollowBrackets True if parentheses are following brackets (indicating a link).
    /// @return Always returns false to reset parsing state for parentheses.
    private boolean handleCloseParenthesis(boolean parensFollowBrackets) {
        closeParenthesis = head;
        if (parensFollowBrackets) {
            saveParensTag();
        }
        return false;
    }

    /// Saves any text from the tail position up to the current head as a TEXT token.
    /// Does nothing if no text is available in that range.
    private void saveText() {
        Token token = new Token(TokenKind.TEXT);
        String text = markdown.substring(tail, head);
        if (text.isEmpty()) return;
        token.setText(text);
        saveToken(token);
    }

    /// Saves the content between the most recent pair of brackets as a BRACKETS_TAG token.
    private void saveBracketsTag() {
        Token token = new Token(TokenKind.BRACKETS_TAG);
        String text = markdown.substring(openBracket + 1, closeBracket);
        token.setText(text);
        saveToken(token);
    }

    /// Saves the content between the most recent pair of parentheses as a PARENS_TAG token.
    private void saveParensTag() {
        if (openParenthesis == -1 || closeParenthesis == -1) {
            return;
        }
        Token token = new Token(TokenKind.PARENS_TAG);
        String text = markdown.substring(openParenthesis + 1, closeParenthesis);
        token.setText(text);
        saveToken(token);
    }

    /// Adds the specified token to the list and links it to the previously saved token.
    /// @param token The token to save and link.
    private void saveToken(Token token) {
        tokens.add(token);
        if (prev != null) {
            prev.setNext(token);
        }
        prev = token;
    }

    /// Returns the first token in the parsed sequence.
    /// If no tokens exist, returns an END kind token.
    /// @return The first token or an END token if none exist.
    public Token firstToken() {
        if (!tokens.isEmpty()) {
            return tokens.getFirst();
        }
        return new Token(TokenKind.END);
    }

    /// Represents a token of the parsed Markdown input.
    /// A token has a kind and associated text content, and links to the next token in sequence.
    public class Token {
        Token next = null;
        TokenKind kind;
        String text = "";

        /// Creates a token with the specified kind.
        /// @param k The token kind.
        public Token(TokenKind k) {
            kind = k;
        }

        /// Returns the token kind.
        /// @return The token kind.
        public TokenKind getKind() {
            return kind;
        }

        /// Sets the text content of the token.
        /// @param t The text string.
        public void setText(String t) {
            text = t;
        }

        /// Returns the text content of the token.
        /// @return The text string.
        public String getText() {
            return text;
        }

        /// Sets the next token in the sequence.
        /// @param token The next token.
        public void setNext(Token token) {
            next = token;
        }

        /// Returns the next token, or an END token if none exists.
        /// @return The next token or an END token.
        public Token getNext() {
            if (next == null) {
                return new Token(TokenKind.END);
            }
            return next;
        }
    }

    /// Enum representing the kind of a Markdown token.
    public enum TokenKind {
        /// No specific kind assigned.
        NONE,

        /// Plain text token.
        TEXT,

        /// Content inside square brackets.
        BRACKETS_TAG,

        /// Content inside parentheses following brackets.
        PARENS_TAG,

        /// End of sequence marker.
        END
    }
}