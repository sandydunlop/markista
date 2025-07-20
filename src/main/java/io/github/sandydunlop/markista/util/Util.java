package io.github.sandydunlop.markista.util;

import io.github.sandydunlop.markista.model.Reference;

/// A set of utility methods for changing between qualified and unqualified names.
public class Util {

    private Util(){
        // This hides the public constructor
    }

    /// Create a markdown link, automatically deciding what kind of link to make
    /// @param identifier a package, type, or method identifier
    /// @param simplify if true, the fully simplified version of the identifier is shown
    /// @return markdown text for a link to a document for the specified identifier or an anchor link
    public static String mdAutoLink(String identifier, boolean simplify) {
        boolean isMethod = identifier.indexOf('(') > -1;
        String name = Util.removeParentheses(identifier);
        Reference link = LinkResolver.resolve(name);
        String pre = "";
        String text;
        if (name.indexOf('<') > -1) {
            text = escape(simplify ? Util.linkGenerics(name, simplify) : name);
            return text;
        } else if (name.indexOf(',') > -1) {
            return splitAndLink(name);
        } else if (name.lastIndexOf(' ') > 0) {
            int p = name.lastIndexOf(' ');
            pre = name.substring(0, p) + " ";
            name = name.substring(p + 1);
            link = LinkResolver.resolve(name);
        }
        text = escape(simplify ?  Util.simplifyNames(name) : name);
        if (isMethod) {
            return String.format("%s[%s](#%s)", pre, text, text);
        } else if (link.kind == Reference.Kind.TYPE) {
            return String.format("%s[%s](%s.md)", pre, text, link.uri);
        } else if (link.kind == Reference.Kind.PACKAGE) {
            return String.format("%s[%s](%s/index.md)", pre, text, link.uri);
        } else if (link.kind == Reference.Kind.URL) {
            return String.format("%s[%s](%s)", pre, text, link.uri);
        } else {
            return String.format("%s%s", pre, text);
        }
    }

    public static String splitAndLink(String typesString) {
        StringBuilder r = new StringBuilder();
        String[] types = typesString.split(",");
        for (String t : types) {
            String typeName = t.strip();
            if (!r.isEmpty()) {
                r.append(", ");
            }
            r.append(mdAutoLink(typeName));
        }
        return r.toString();
    }

    /// Changes qualified generic type names to unqualified generic type names and adds links to their API documentation.
    /// @param str A string containing a qualified generic name.
    /// @return    A string with the qualified names changed to unqualified names and links to types added
    public static String linkGenerics(String str, boolean simplifiy) {
        if (str == null || str.isEmpty()) return str;
        int start = str.indexOf("<");
        if (start > -1) {
            int end = str.indexOf(">");
            if (end > start) {
                String before = str.substring(0, start);
                before = mdAutoLink(before, simplifiy);
                String after = str.substring(end + 1);
                String mid = str.substring(start + 1, end);
                String simplified = splitAndLink(mid);
                return before + "<" + simplified + ">" + after;
            }
        }
        return mdAutoLink(str, simplifiy);
    }

    /// Removes parentheses and what they contain from an expression
    /// @param expression An expression such as `classname.method(parameter)`.
    /// @return The expression with the parentheses removed
    public static String removeParentheses(String expression) {
        int start = expression.indexOf('(');
        if (start > -1) {
            int end = expression.indexOf(')', start);
            String r = "";
            if (start > 0) {
                r = expression.substring(0, start);
            }
            if (end < expression.length()) {
                r += expression.substring(end + 1);
            }
            return r;
        }
        return expression;
    }

    /// Changes all qualified names in a string into unqualified names.
    /// @param  str A string that may contain one or more qualified names.
    /// @return The input string, with all qualified names changed to unqualified names.
    public static String simplifyNames(String str) {
        String simplified = str;
        if (simplified == null || simplified.isEmpty()) return "";
        int qualifiedStart;
        int simpleStart;
        char prev = (char)0;
        qualifiedStart = -1;
        simpleStart = -1;
        for (int i=0; i<=simplified.length(); i++) {
            char c = i<simplified.length() ? simplified.charAt(i) : ' ';
            if (qualifiedStart > -1 && simpleStart > -1 && (i == simplified.length()  || !isValidSimpleNameChar(c))){
                String tmp = "";
                if (qualifiedStart > 0) tmp = simplified.substring(0, qualifiedStart);
                tmp += simplified.substring(simpleStart);
                simplified = tmp;
                i = qualifiedStart + (i-simpleStart);
                simpleStart = -1;
                qualifiedStart = -1;
            }  else if (simpleStart > -1 && isValidSimpleNameChar(c)) {
                // skip
            } else if (qualifiedStart == -1 && isValidQualifiedNameChar(c) && !isValidSimpleNameChar(prev)) {
                qualifiedStart = i;
            } else if (qualifiedStart > -1 && simpleStart == -1 && Character.isUpperCase(c)) {
                simpleStart = i;
            } else if (qualifiedStart > -1 && simpleStart == -1 && !isValidQualifiedNameChar(c)) {
                qualifiedStart = -1;
            }
            prev = c;
        }
        return simplified;
    }

    /// Checks if the given character is valid in an unqualified name.
    /// @param c The character to check.
    /// @return  Whether or not the character is valid in an unqualified name.
    public static boolean isValidSimpleNameChar(char c){
        return Character.isAlphabetic(c) || c == '.';
    }

    /// Checks if the given character is valid in a qualified name.
    /// @param c The character to check.
    /// @return  Whether or not the character is valid in a qualified name.
    public static boolean isValidQualifiedNameChar(char c){
        return (Character.isAlphabetic(c) && Character.isLowerCase(c)) || c == '.';
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static String inOneLine(String text) {
        if (text == null) return "";
        return text.replace("\n", " ");
    }

    public static String mdAnchor(String phrase) {
        return phrase.toLowerCase().replace(" ","");
    }

    public static String mdAnchorLink(String phrase){
        return "[" + phrase + "](#" + mdAnchor(phrase) + ")";
    }

    public static String mdDocumentLink(String docName) {
        return mdDocumentLink(docName, docName);
    }

    public static String mdDocumentLink(String phrase, String docName) {
        if (docName.contains("://") || docName.endsWith(".md")){
            return String.format("[%s](%s)", phrase, docName);
        }
        return String.format("[%s](%s.md)", phrase, docName);
    }

    public static String mdAutoLink(String identifier) {
        return mdAutoLink(identifier, true);
    }

    public static String escape(String str) {
        return str
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
