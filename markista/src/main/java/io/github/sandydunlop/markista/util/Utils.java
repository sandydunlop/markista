package io.github.sandydunlop.markista.util;

import io.github.sandydunlop.markista.model.Text;

/// A set of utility methods for changing between qualified and unqualified names.
public class Utils {

    private Utils(){
        // This hides the public constructor
    }

    /// Removes the generic type and its surrounding <> from a string, if present
    /// @param str The string
    /// @return The string with the generic type and surrounding <> removed
    public static String removeGenerics(String str) {
        if (isNullOrEmpty(str)) return "";
        int start = str.indexOf("<");
        if (start > -1) {
            int end = str.indexOf(">");
            if (end > start) {
                return str.substring(0, start);
            }
        }
        return str;
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
        if (isNullOrEmpty(str)) return "";
        return simplifyNamesLoop(str);
    }

    private static String simplifyNamesLoop(String input) {
        String simplified = input;
        int qualifiedStart = -1;
        int simpleStart = -1;
        char prev = (char)0;
        int i = 0;
        while (i <= simplified.length()) {
            char c = i < simplified.length() ? simplified.charAt(i) : ' ';
            if (shouldReplaceQualifiedWithSimple(qualifiedStart, simpleStart, i, simplified.length(), c)) {
                simplified = replaceQualifiedWithSimple(simplified, qualifiedStart, simpleStart, i);
                i = qualifiedStart + (i - simpleStart);
                simpleStart = -1;
                qualifiedStart = -1;
            } else if (qualifiedStart == -1 && isValidQualifiedNameChar(c) && !isValidSimpleNameChar(prev)) {
                qualifiedStart = i;
            } else if (qualifiedStart > -1 && simpleStart == -1 && Character.isUpperCase(c)) {
                simpleStart = i;
            } else if (qualifiedStart > -1 && simpleStart == -1 && !isValidQualifiedNameChar(c)) {
                qualifiedStart = -1;
            }
            prev = c;
            i++;
        }
        return simplified;
    }

    private static boolean shouldReplaceQualifiedWithSimple(int qualifiedStart, int simpleStart, int i, int length, char c) {
        return qualifiedStart > -1 && simpleStart > -1 && (i == length || !isValidSimpleNameChar(c));
    }

    private static String replaceQualifiedWithSimple(String simplified, int qualifiedStart, int simpleStart, int i) {
        StringBuilder tmp = new StringBuilder();
        if (qualifiedStart > 0) tmp.append(simplified, 0, qualifiedStart);
        tmp.append(simplified.substring(simpleStart, i));
        if (i < simplified.length()) tmp.append(simplified.substring(i));
        return tmp.toString();
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

    /// Checks if a string is null or empty
    /// @param str The string
    /// @return True if the string is either null or empty
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /// Checks if a Text object is null or empty
    /// @param text The Text object
    /// @return True if the Text object is either null or empty
    public static boolean isNullOrEmpty(Text text) {
        return text == null || text.isEmpty();
    }

    /// Removes new line characters from a string, replacing them with spaces
    /// @param str The string
    /// @return The string, with newlines converted to spaces
    public static String inOneLine(String str) {
        if (isNullOrEmpty(str)) return "";
        return str.replace("\n", " ");
    }

}
