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
    private static final Context ctx = Context.getInstance();

    private Markdown() {
        // This hides the public constructor
    }

    /// Formats the signature of a method as markdown.
    /// @param method The method to be formatted as markdown
    /// @return Markdown formatted text representing the method's signature
    public static String fullSignature(MethodNode method ) {
        String sig = method.getModifiersString();
        sig += link(Reference.to(method.getReturnType().getQualifiedName()), false) + " ";
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
            String typeName = link(Reference.to(param.getType().getQualifiedName()), false);
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
        if (ref.getKind() == Reference.Kind.PACKAGE || ref.getKind() == Reference.Kind.TYPE) {
            return link(ref);
        }
        if (ref.getLabel() == null || ref.getLabel().isEmpty()) {
            ref.setLabel(ref.getTarget());
        }
        if (ref.getKind() == Reference.Kind.URL) {
            return mdDocumentLink(ref.getUri());
        } else if (ref.getKind() == Reference.Kind.PAGE) {
            String relativePath = LinkResolver.relativize("");
            return mdDocumentLink(ref.getLabel(), Path.of(relativePath, ref.getUri()).toString());
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
                    sb.append(segment);
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
                    ctx.reportWarning("Unhandled javadoc tag:\n  " + segment.getKind().toString() + "\n  " + segment);
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
                    Reference ref = Reference.to(next.getText());
                    ref.setLabel(segment.getText());
                    String link = link(ref, false);
                    if (link.isEmpty()) {
                        sb.append(next.getText());
                    } else {
                        sb.append(link);
                    }
                    segment = next;
                } else {
                    sb.append(link(Reference.to(segment.getText())));
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
        if (segment.getLink().contains("://")) {
            Reference ref = new Reference();
            ref.setKind(Reference.Kind.URL);
            ref.setLabel(segment.getText());
            ref.setUri(segment.getLink());
            return mdRefLink(ref);
        }
        if (segment.getText().isBlank()) {
            return link(Reference.to(segment.getLink()));
        } else {
            Reference ref = LinkResolver.resolve(Reference.to(segment.getLink()));
            ref.setLabel(segment.getText());
            return mdRefLink(ref);
        }
    }

    /// Create a markdown link, automatically deciding what kind of link to make.
    /// @param link a Reference object describing the link
    /// @return markdown formatted link
    public static String link(Reference link) {
        return link(link, false);
    }

    /// Create a markdown link, automatically deciding what kind of link to make.
    /// @param reference a Reference object describing the link
    /// @param useQualifiedName If true, qualified names will be used in the link label
    /// @return markdown formatted link
    public static String link(Reference reference, boolean useQualifiedName) {
        String targetName = reference.getTarget();
        if (targetName == null || targetName.isEmpty()) {
            ctx.reportWarning("No link target supplied");
            return reference.getLabel();
        }
        String pre = "";
        String post = "";
        String anchor = "";
        boolean isLocalMethod = false;
        String displayName = reference.getLabel();
        int pos = targetName.indexOf('#');
        if (pos == 0) {
            return mdAnchorLink(targetName);
        }
        if (pos > 0) {
            anchor = targetName.substring(pos);
            targetName = targetName.substring(0, pos);
        }
        if (targetName.indexOf('<') > -1) {
            return escape(linkGenerics(targetName, useQualifiedName));
        } else if (targetName.indexOf(',') > -1) {
            return splitAndLink(targetName, useQualifiedName);
        } else if (targetName.lastIndexOf(' ') > 0) {
            int p = targetName.lastIndexOf(' ');
            pre = targetName.substring(0, p) + " ";
            targetName = targetName.substring(p + 1);
        }
        pos = targetName.indexOf('[');
        if (pos > 0) {
            post = targetName.substring(pos);
            targetName = targetName.substring(0, pos);
        }
        if (targetName.contains("(")) {
            targetName = targetName.substring(0, targetName.indexOf("("));
        }
        reference.setTarget(targetName);
        reference.setAnchor(anchor);
        if (reference.getLabel() == null || reference.getLabel().isEmpty()) {
            reference.setLabel(reference.getTarget());
        }
        LinkResolver.resolve(reference);
        if (!anchor.isEmpty() && reference.getKind() != Reference.Kind.URL) {
            isLocalMethod = true;
            // Issue: https://github.com/sandydunlop/markista/issues/1
            // Workaround:
            // Remove the parentheses from after method names in anchor 
            // links to Markdown pages for now. Anchors in the Markdown
            // are currently headings without parameters.
            reference.setAnchor(Utils.removeParentheses(reference.getAnchor()));
        }
        setDisplayName(reference, displayName, isLocalMethod, useQualifiedName);
        return pre + mdRefLink(reference) + post;
    }

    private static void setDisplayName(Reference link, String displayName, boolean isLocalMethod, boolean useQualifiedName) {
        if (link.getLabel() == null) {
             link.setLabel(link.getTarget());
        }
        if (isLocalMethod) {
            link.setKind(Reference.Kind.METHOD);
            String methodName = link.getAnchor().substring(1);
            if (!link.getClassName().equals(ctx.getTypeName())) {
                link.setLabel(link.getLabel() + "." + methodName);
            } else {
                link.setLabel(methodName);
            }
        }
        if (displayName != null && !displayName.isEmpty()) {
            link.setLabel(displayName.replace("#","."));
        } else if (!useQualifiedName && canBeSimplified(link)) {
            link.setLabel(Utils.simplifyNames(link.getLabel()));
        }
        link.setLabel(escape(link.getLabel()));
    }

    private static boolean canBeSimplified(Reference link) {
        boolean originIsInPackage = !ctx.getPackageName().isEmpty();
        boolean kindCanBeSimplified = link.getKind() == Reference.Kind.TYPE || link.getKind() == Reference.Kind.METHOD || link.getKind() == Reference.Kind.URL;
        return kindCanBeSimplified && originIsInPackage;
    }


    /// Creates a markdown formatted link from a [Reference] object.
    /// @param link The reference object
    /// @return a markdown formatted link
    public static String mdRefLink(Reference link) {
        if (link.getKind() == Reference.Kind.METHOD) {
            return mdRefLinkMethod(link);
        } else if (link.getKind() == Reference.Kind.TYPE) {
            return String.format("[%s](%s.md%s)", link.getLabel(), link.getUri(), mdAnchor(link.getAnchor()));
        } else if (link.getKind() == Reference.Kind.PACKAGE) {
            return String.format("[%s](%s/index.md%s)", link.getLabel(), link.getUri(), mdAnchor(link.getAnchor()));
        } else if (link.getKind() == Reference.Kind.MODULE) {
            return String.format("[%s](%s/index.md)", link.getLabel(), link.getUri());
        } else if (link.getKind() == Reference.Kind.URL) {
            String displayName = link.getLabel();
            if (!link.getAnchor().isEmpty() && link.getAnchor().length() > 1) {
                String anchorName = link.getAnchor().substring(1);
                displayName += "." + anchorName;
            }
            return String.format("[%s](%s%s)", displayName, link.getUri(), link.getAnchor());
        }
        return link.getLabel();
    }

    /// Creates a markdown formatted link from a [Reference] object.
    /// with the option of simplifying qualified type names.
    /// @param link The reference object
    /// @return a markdown formatted link
    public static String mdRefLinkMethod(Reference link) {
        link.setAnchor(link.getAnchor().toLowerCase());
        String displayName = link.getLabel();
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
    /// @param useQualifiedName If true, qualified type names will be displayed
    /// @return    A string with the qualified names changed to unqualified
    ///            names and links to types added
    public static String linkGenerics(String str, boolean useQualifiedName) {
        if (str == null || str.isEmpty()) return str;
        int openingChevron = str.indexOf("<");
        int closingChevron = str.lastIndexOf(">");
        String before = str.substring(0, openingChevron);
        String mid = str.substring(openingChevron + 1, closingChevron);
        String after = str.substring(closingChevron + 1);
        before = link(Reference.to(before), useQualifiedName);
        mid = splitAndLink(mid, useQualifiedName);
        return before + "&lt;" + mid + "&gt;" + after;
    }

    /// Creates markdown formatted text with links to types from a string.
    /// containing one or more types separated by commas.
    /// @param typesString A string containing a comma-separated list of type names
    /// @param useQualifiedName if true, the qualified version of the identifier is shown
    /// @return a list of links to types formatted as Markdown
    public static String splitAndLink(String typesString, boolean useQualifiedName) {
        StringBuilder r = new StringBuilder();
        String[] types = typesString.split(",");
        for (String t : types) {
            String typeName = t.strip();
            if (!r.isEmpty()) {
                r.append(", ");
            }
            r.append(link(Reference.to(typeName), useQualifiedName));
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
