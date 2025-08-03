package io.github.sandydunlop.markista.util;

import java.nio.file.Path;
import java.util.List;

import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.util.MarkdownParser.SegmentKind;

/// A utility class for producing Markdown formatted text and resolving
/// Markdown links to point to the correct file, directory, or web page.
public class Markdown {
    private static final String FORMAT_SIMPLE_LINK = "[%s](%s)";

    /// The Context singleton instance providing access to the current documentation generation context,
    /// including configuration, current module/package/type names, and reporting utilities.
    private static Context ctx = Context.getInstance();

    private Markdown() {
        // This hides the public constructor
    }

    /// Formats the signature of a method as markdown.
    /// @param method The method to be formatted as markdown
    /// @return Markdown formatted text representing the method's signature
    public static String fullSignature(MethodNode method ) {
        String sig = method.getModifiersString();
        sig += mdAutoLink(method.getReturnType().getQualifiedName(), true) + " ";
        sig += method.getSimpleName() + "(" + formatParams(method.getParams()) + ")";
        return sig;
    }

    /// Formats a list of `ParamNode` objects as markdown, identifying and linking type names.
    /// @param params a `Reference` object specifying a link
    /// @return Markdown formatted text containing a link
    public static String formatParams(List<ParamNode> params){
        StringBuilder sb = new StringBuilder();
        int paramCount = 0;
        for (ParamNode param : params) {
            if (paramCount++ > 0) sb.append(", ");
            String typeName = mdAutoLink(param.getType().getQualifiedName(), true);
            sb.append(typeName);
            sb.append(param.getType().getArrayBrackets());
            sb.append(" ");
            sb.append(param.getSimpleName()); 
        }
        return sb.toString();
    }

    /// Formats a link specified in a `Reference` object as markdown.
    /// @param ref a `Reference` object specifying a link
    /// @return Markdown formatted text containing a link
    public static String formatReference(Reference ref) {
        if (ref.getDisplayName() == null || ref.getDisplayName().isEmpty()) {
            ref.setDisplayName(ref.getUri());
        }
        if (ref.getKind() == Reference.Kind.URL) {
            return mdDocumentLink(ref.getUri());
        } else if (ref.getKind() == Reference.Kind.PAGE) {
            String relativePath = LinkResolver.relativize("");
            return mdDocumentLink(ref.getDisplayName(), Path.of(relativePath, ref.getUri()).toString());
        } else if (ref.getKind() == Reference.Kind.PACKAGE || ref.getKind() == Reference.Kind.TYPE) {
            return mdAutoLink(ref.getDisplayName());
        }
        return "";
    }

    /// Formats text contained in a `Text` object as markdown.
    /// @param text A `Text` object to be formatted
    /// @return Markdown formatted text
    public static String formatText(Text text) {
        if (text == null) return "";
        List<Text.Segment> parsedSegments = text.getSegments();
        if (parsedSegments == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Text.Segment segment : parsedSegments) {
            switch (segment.getKind()) {
                case Text.SegmentKind.TEXT, Text.SegmentKind.END:
                    sb.append(segment.toString());
                    break;
                case Text.SegmentKind.MARKDOWN:
                    sb.append(resolveMarkdownLinks(segment.toString()));
                    break;
                case Text.SegmentKind.CODE:
                    sb.append("`");
                    sb.append(segment.getText());
                    sb.append("`");
                    break;
                case Text.SegmentKind.LINK:
                    sb.append(formatLink(segment));
                    break;
                case Text.SegmentKind.START:
                    break;
                default:
                    ctx.reportWarning("Unhandled javadoc tag:\n  " + segment.getKind().toString() + "\n  " + segment.toString());
            }
        }
        return sb.toString();
    }

    /// Resolves markdown formatted links to point to the correct directory and page.
    /// @param markdown Markdown formatted text containing links
    /// @return markdown formatted text with resolved links
    public static String resolveMarkdownLinks(String markdown) {
        StringBuilder sb = new StringBuilder();
        MarkdownParser parser = new MarkdownParser(markdown);
        MarkdownParser.Segment segment = parser.firstSegment();
        while (segment.getKind() != MarkdownParser.SegmentKind.END) {
            if (segment.getKind() == MarkdownParser.SegmentKind.BRACKETS_TAG) {
                MarkdownParser.Segment next = segment.getNext();
                if (next.getKind() == SegmentKind.BRACKETS_TAG || next.getKind() == SegmentKind.PARENS_TAG) {
                    String link = mdAutoLink(next.getText(), segment.getText(), false);
                    if (link.isEmpty()) {
                        sb.append(next.getText());
                    } else {
                        sb.append(link);
                    }
                    segment = next;
                } else {
                    sb.append(mdAutoLink(segment.getText()));
                }
            } else if (segment.getKind() == SegmentKind.TEXT) {
                sb.append(segment.getText());
            }
            segment = segment.getNext();
        }
        return sb.toString();
    }

    /// Formats links contained in a text segment as markdown.
    /// @param segment A text segment
    /// @return Markdown formatted text with a resolved link
    public static String formatLink(Text.Segment segment) {
        if (segment.getLink().indexOf("://") > -1) {
            Reference ref = new Reference();
            ref.setKind(Reference.Kind.URL);
            ref.setDisplayName(segment.getText());
            ref.setUri(segment.getLink());
            return mdRefLink(ref);
        }
        if (segment.getText().isBlank()) {
            return mdAutoLink(segment.getLink());
        } else {
            Reference ref = LinkResolver.resolve(segment.getLink());
            ref.setDisplayName(segment.getText());
            return mdRefLink(ref);
        }
    }

    /// Create a markdown link, automatically deciding what kind of link to make.
    /// @param identifier a package, type, or method identifier
    /// @return markdown text for a link to a document for the specified identifier or an anchor link
    public static String mdAutoLink(String identifier) {
        return mdAutoLink(identifier, true);
    }

    /// Create a markdown link, automatically deciding what kind of link to make.
    /// @param identifier a package, type, or method identifier
    /// @param simplify if true, the fully simplified version of the identifier is shown
    /// @return markdown text for a link to a document for the specified identifier or an anchor link
    public static String mdAutoLink(String identifier, boolean simplify) {
        return mdAutoLink(identifier, null, simplify);
    }

    /// Create a markdown link, automatically deciding what kind of link to make.
    /// @param identifier a package, type, or method identifier
    /// @param displayName If non-null, `displayName` will be the text displayed in the generated markdown.
    /// @param simplify if true, the simplified version of the identifier is shown
    /// @return markdown text for a link to a document for the specified identifier or an anchor link
    public static String mdAutoLink(String identifier, String displayName, boolean simplify) {
        boolean isLocalMethod = false;
        String name = identifier;
        String pre = "";
        String post = "";
        String anchor = "";
        int pos = name.indexOf('#');
        if (pos == 0) {
            return mdAnchorLink(name);
        }
        if (pos > 0) {
            anchor = name.substring(pos);
            name = name.substring(0, pos);
        }
        if (name == null) {
            return identifier;
        } else if (name.indexOf('<') > -1) {
            return escape(linkGenerics(name, simplify));
        } else if (name.indexOf(',') > -1) {
            return splitAndLink(name, simplify);
        } else if (name.lastIndexOf(' ') > 0) {
            int p = name.lastIndexOf(' ');
            pre = name.substring(0, p) + " ";
            name = name.substring(p + 1);
        }
        pos = name.indexOf('[');
        if (pos > 0) {
            post = name.substring(pos);
            name = name.substring(0, pos);
        }
        if (name.indexOf("(") > -1) {
            name = name.substring(0, name.indexOf("("));
        }
        Reference link = LinkResolver.resolve(name);
        link.setAnchor(anchor);
        if (!anchor.isEmpty() && link.getKind() != Reference.Kind.URL) {
            isLocalMethod = true;
            // Issue: https://github.com/sandydunlop/markista/issues/1
            // Workaround:
            // Remove the parentheses from after method names in anchor 
            // links to Markdown pages for now. Anchors in the Markdown
            // are currently headings without parameters.
            link.setAnchor(Utils.removeParentheses(link.getAnchor()));
        }
        setDisplayName(link, displayName, isLocalMethod, simplify);
        if (isLocalMethod) {
            link.setKind(Reference.Kind.METHOD);
        }
        return pre + mdRefLink(link) + post;
    }

    private static void setDisplayName(Reference link, String displayName, boolean isLocalMethod, boolean simplify) {
        if (displayName == null) {
            displayName = link.getDisplayName();
            if (isLocalMethod) {
                String methodName = link.getAnchor().substring(1);
                String ctn = ctx.getTypeName();
                if (!link.getClassName().equals(ctn)) {
                    displayName = link.getClassName() + "." + methodName;
                } else {
                    displayName = methodName;
                }
            }
        } else {
            link.setDisplayName(displayName);
        }
        if (simplify) {
            link.setDisplayName(escape(Utils.simplifyNames(displayName)));
        } else {
            link.setDisplayName(escape(displayName));
        }
    }


    /// Creates a markdown formatted link from a [Reference] object.
    /// @param link The reference object
    /// @return a markdown formatted link
    public static String mdRefLink(Reference link) {
        if (link.getKind() == Reference.Kind.METHOD) {
            return mdRefLinkMethod(link);
        } else if (link.getKind() == Reference.Kind.TYPE) {
            return String.format("[%s](%s.md%s)", link.getDisplayName(), link.getUri(), mdAnchor(link.getAnchor()));
        } else if (link.getKind() == Reference.Kind.PACKAGE) {
            return String.format("[%s](%s/index.md%s)", link.getDisplayName(), link.getUri(), mdAnchor(link.getAnchor()));
        } else if (link.getKind() == Reference.Kind.MODULE) {
            return String.format("[%s](%s/index.md)", link.getDisplayName(), link.getUri());
        } else if (link.getKind() == Reference.Kind.URL) {
            String displayName = link.getDisplayName();
            if (!link.getAnchor().isEmpty() && link.getAnchor().length() > 1) {
                String anchorName = link.getAnchor().substring(1);
                displayName += "." + anchorName;
            }
            return String.format("[%s](%s%s)", displayName, link.getUri(), link.getAnchor());
        }
        return link.getDisplayName();
    }

    /// Creates a markdown formatted link from a [Reference] object.
    /// with the option of simplifying qualified type names.
    /// @param link The reference object
    /// @return a markdown formatted link
    public static String mdRefLinkMethod(Reference link) {
        link.setAnchor(link.getAnchor().toLowerCase());
        String displayName = link.getDisplayName();
        String ctn = ctx.getTypeName();
        if (!link.getClassName().equals(ctn)) {
            return String.format("[%s](%s.md%s)", displayName, link.getUri(), link.getAnchor());
        } else {
            return String.format(FORMAT_SIMPLE_LINK, displayName, link.getAnchor());
        }
    }

    /// Changes qualified generic type names to unqualified generic 
    /// type names and adds links to their API documentation.
    /// @param str A string containing a qualified generic name.
    /// @param simplify If true, qualified type names will be simplified
    /// @return    A string with the qualified names changed to unqualified
    ///            names and links to types added
    public static String linkGenerics(String str, boolean simplify) {
        if (str == null || str.isEmpty()) return str;
        int openingChevron = str.indexOf("<");
        int closingChevron = str.lastIndexOf(">");
        String before = str.substring(0, openingChevron);
        String mid = str.substring(openingChevron + 1, closingChevron);
        String after = str.substring(closingChevron + 1);
        before = mdAutoLink(before, simplify);
        mid = splitAndLink(mid, simplify);
        return before + "&lt;" + mid + "&gt;" + after;
    }

    /// Creates markdown formatted text with links to types from a string.
    /// containing one or more types separated by commas.
    /// @param typesString A string containing a comma-separated list of type names
    /// @param simplify if true, the simplified version of the identifier is shown
    /// @return a list of links to types formatted as Markdown
    public static String splitAndLink(String typesString, boolean simplify) {
        StringBuilder r = new StringBuilder();
        String[] types = typesString.split(",");
        for (String t : types) {
            String typeName = t.strip();
            if (!r.isEmpty()) {
                r.append(", ");
            }
            r.append(mdAutoLink(typeName, simplify));
        }
        return r.toString();
    }

    /// Converts a string to the format required for use as a Markdown anchor.
    /// Converted strings will be lowercase and contain no spaces.
    /// @param phrase A string to be used in an anchor link
    /// @return The string converted to match the format required by Markdown anchor links
    public static String mdAnchor(String phrase) {
        return phrase.toLowerCase().replace(" ","");
    }

    /// Create a markdown formatted link to an anchor within the same markdown page.
    /// @param phrase A string to be used in an anchor link
    /// @return Markdown formatted text containing a correctly formatted anchor link
    public static String mdAnchorLink(String phrase){
        if (Utils.isNullOrEmpty(phrase)) return "";
        if (phrase.length() > 1 && phrase.charAt(0) == '#') {
            phrase = phrase.substring(1);
        }
        // Issue: https://github.com/sandydunlop/markista/issues/1
        // Workaround:
        // Remove the parentheses from after method names in anchor 
        // links to Markdown pages for now. Anchors in the Markdown
        // are currently headings without parameters.
        return "[" + phrase + "](#" + mdAnchor(Utils.removeParentheses(phrase)) + ")";
    }

    /// Creates a Markdown link to another Markdown document
    /// @param docName The filename of the document being linked to
    /// @return The Markdown formatted link
    public static String mdDocumentLink(String docName) {
        return mdDocumentLink(docName, docName);
    }

    /// Creates a Markdown formatted link to another Markdown page or a web page
    /// @param phrase the text displayed for the link in the Markdown page
    /// @param docName the name of the Markdown page being linked to, or the 
    ///                URL of a web page being linked to
    /// @return Markdown formatted text containing a correctly formatted link to
    ///         the specified Markdown page or web page.
    public static String mdDocumentLink(String phrase, String docName) {
        if (docName.contains("://") || docName.endsWith(".md")){
            return String.format(FORMAT_SIMPLE_LINK, phrase, docName);
        }
        return String.format("[%s](%s.md)", phrase, docName);
    }

    /// Escapes HTML `<` and `>` characters in a string with their corresponding
    /// HTML character entities, `&lt;` and `&gt;`.
    /// @param str A string to be escaped
    /// @return The escaped string
    public static String escape(String str) {
        return str
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
