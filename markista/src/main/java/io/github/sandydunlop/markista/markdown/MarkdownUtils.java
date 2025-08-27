package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.common.Utils;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.Link;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.TypeReference;

import java.util.List;

/// A utility class for producing Markdown formatted text and resolving
/// Markdown links to point to the correct file, directory, or web page.
public class MarkdownUtils {
    private static final String FORMAT_SIMPLE_LINK = "[%s](%s)";

    /// The Context singleton instance providing access to the current documentation generation context,
    /// including configuration, current module/package/type names, and reporting utilities.
    private static Context ctx = Context.getInstance();

    private MarkdownUtils() {
        // This hides the public constructor
    }

    public static void setContext(Context c) {
        ctx = c;
    }

    /// Formats the signature of a method as markdown.
    /// @param method The method to be formatted as markdown
    /// @return Markdown formatted text representing the method's signature
    public static String fullSignature(MethodNode method ) {
        String sig = method.getModifiersString();
        sig += formatTypeRef(method.getReturnType()) + " ";
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
            String typeName = formatTypeRef(param.getType());
            sb.append(typeName);
            sb.append(" ");
            sb.append(param.getSimpleName()); 
        }
        return sb.toString();
    }

    /// Formats text contained in a `Text` object as markdown.
    /// @param text A `Text` object to be formatted
    /// @return Markdown formatted text
    public static String formatText(Text text) {
        return formatText(text, false, true);
    }

    public static String formatText(Text text, boolean qualifyType, boolean qualifyMember) {
        if (text == null) return "";
        List<Text.Segment> parsedSegments = text.getSegments();
        if (parsedSegments == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Text.Segment segment : parsedSegments) {
            switch (segment.getKind()) {
                case Text.Segment.Kind.TEXT:
                    sb.append(segment);
                    break;
                case Text.Segment.Kind.CODE:
                    sb.append("`");
                    sb.append(segment.getText());
                    sb.append("`");
                    break;
                case Text.Segment.Kind.LINK:
                    sb.append(formatLink(segment, qualifyType, qualifyMember));
                    break;
                default:
                    ctx.reportWarning("Unhandled javadoc tag:\n  " + segment.getKind().toString() + "\n  " + segment);
            }
        }
        return sb.toString();
    }

    public static String formatTypeRef(TypeReference typeRef) {
        return formatTypeRef(typeRef, false);
    }

    public static String formatTypeRef(TypeReference typeRef, boolean useQualifiedName) {
        return switch (typeRef) {
            case TypeReference.Array array -> {
                String arrayType = link(typeRef.getLink(), useQualifiedName);
                for (int i = 0; i < array.getDimensions(); i++) {
                    arrayType += "[]";
                }
                yield arrayType;
            }
            case TypeReference.Generic generic -> {
                StringBuilder sb = new StringBuilder();
                sb.append(link(typeRef.getLink(), useQualifiedName));
                sb.append("<");
                if (generic.hasWildcard()) {
                    sb.append("?");
                } else if (generic.hasExtendsWildcard()) {
                    sb.append("? extends ");
                }
                sb.append(formatTypeRef(generic.getParams(), useQualifiedName));
                sb.append(">");
                yield sb.toString();
            }
            case TypeReference.Sequence sequence -> {
                StringBuilder sb = new StringBuilder();
                for (TypeReference item : sequence.getItems()) {
                    if (!sb.isEmpty()) {
                        sb.append(", ");
                    }
                    sb.append(formatTypeRef(item, useQualifiedName));
                }
                yield sb.toString();
            }
            default -> {
                String s = link(typeRef.getLink(), useQualifiedName);
                yield s;
            }
        };
    }

    /// Formats links contained in a text segment as markdown.
    /// @param segment A text segment
    /// @return Markdown formatted text with a resolved link
    public static String formatLink(Text.Segment segment, boolean qualifyType, boolean qualifyMember) {
        if (segment.getLink().getLabel() == null || segment.getLink().getLabel().isEmpty()) {
            segment.getLink().setLabel(segment.getText());
        }
        if (segment.getLink().getLabel().isEmpty()) {
            segment.getLink().setLabel(segment.getLink().getTarget());
        }
        if (qualifyType) {
            return link(segment.getLink(), segment.getLink().getLabel(), qualifyType, qualifyMember);
        } else {
            return link(segment.getLink(), segment.getText(), qualifyType, qualifyMember);
        }
    }

    /// Create a markdown formatted link
    /// @param reference a Reference object describing the link
    /// @param useQualifiedName If true, qualified names will be used in the link label
    /// @return markdown formatted link
    public static String link(Link reference, boolean useQualifiedName) {
        return link(reference, null, useQualifiedName, true);
    }

    public static String link(Link link, String label, boolean qualifyType, boolean qualifyMember) {
        if (link.getKind() == Link.Kind.METHOD) {
            if (qualifyType) {
                link.setLabel(link.getQualifiedClassName() + "." + link.getMethodName());
            } else if (!qualifyMember) {
                link.setLabel(link.getMethodName());
            } else if (label!= null && !label.isEmpty()) {
                link.setLabel(label);
            }
        } else if (!qualifyType && label!= null && !label.isEmpty()) {
            link.setLabel(label);
        }
        if (!qualifyType && canBeSimplified(link) && link.getKind() != Link.Kind.METHOD) {
            link.setLabel(Utils.simplifyNames(link.getLabel()));
        }
        link.setLabel(escape(link.getLabel()));
        return mdRefLink(link);
    }

    private static boolean canBeSimplified(Link link) {
        boolean originIsInPackage = !ctx.getPackageName().isEmpty();
        boolean kindCanBeSimplified = link.getKind() == Link.Kind.TYPE || link.getKind() == Link.Kind.METHOD || link.getKind() == Link.Kind.URL;
        return kindCanBeSimplified && originIsInPackage;
    }


    /// Creates a markdown formatted link from a [Link] object.
    /// @param link The reference object
    /// @return a markdown formatted link
    public static String mdRefLink(Link link) {
        if (link.getKind() == Link.Kind.METHOD) {
            return mdRefLinkMethod(link);
        } else if (link.getKind() == Link.Kind.TYPE) {
            return String.format("[%s](%s.md%s)", link.getLabel(), link.getUri(), mdAnchor(link.getAnchor()));
        } else if (link.getKind() == Link.Kind.PACKAGE) {
            return String.format("[%s](%s/index.md%s)", link.getLabel(), link.getUri(), mdAnchor(link.getAnchor()));
        } else if (link.getKind() == Link.Kind.MODULE) {
            return String.format("[%s](%s/index.md)", link.getLabel(), link.getUri());
        } else if (link.getKind() == Link.Kind.URL) {
            String displayName = link.getLabel();
            if (!link.getAnchor().isEmpty() && link.getAnchor().length() > 1) {
                String anchorName = link.getAnchor().substring(1);
                displayName += "." + anchorName;
            }
            return String.format("[%s](%s%s)", displayName, link.getUri(), link.getAnchor());
        } else if (link.getKind() == Link.Kind.PAGE) {
            return String.format("[%s](%s.md)", link.getLabel(), link.getUri());

        }
        return link.getLabel();
    }

    /// Creates a markdown formatted link from a [Link] object.
    /// with the option of simplifying qualified type names.
    /// @param link The reference object
    /// @return a markdown formatted link
    public static String mdRefLinkMethod(Link link) {
        link.setAnchor(link.getAnchor().toLowerCase());
        String displayName = link.getLabel();
        String ctn = ctx.getTypeName();
        String linkClassName = link.getQualifiedClassName();
        if (!linkClassName.equals(ctn)) {
            return String.format("[%s](%s.md%s)", displayName, link.getUri(), link.getAnchor());
        } else {
            return String.format(FORMAT_SIMPLE_LINK, displayName, link.getAnchor());
        }
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
        if (phrase == null || phrase.isEmpty()) return "";
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
        if (docName.contains("://") || docName.endsWith(".md")){
            return String.format(FORMAT_SIMPLE_LINK, docName, docName);
        }
        return String.format("[%s](%s.md)", docName, docName);
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

    /// Removes new line characters from a string, replacing them with spaces
    /// @param str The string
    /// @return The string, with newlines converted to spaces
    public static String inOneLine(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.replace("\n", " ");
    }
}
