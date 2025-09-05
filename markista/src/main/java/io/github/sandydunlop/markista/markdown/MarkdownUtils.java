package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.Link;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.TypeReference;
import io.github.sandydunlop.markista.model.TypeReference.TypeParameter;

import java.util.List;

/// A utility class for producing Markdown formatted text and resolving
/// Markdown links to point to the correct file, directory, or web page.
public class MarkdownUtils {
    private static final String FORMAT_SIMPLE_LINK = "[%s](%s)";
    private static final String FORMAT_SIMPLE_LINK_MD = "[%s](%s.md)";

    /// The Context singleton instance providing access to the current documentation generation context,
    /// including configuration, current module/package/type names, and reporting utilities.
    private static Context ctx = Context.getInstance();

    private MarkdownUtils() {
        // This hides the public constructor
    }

    public static void setContext(Context c) {
        ctx = c;
    }

    /// Formats a list of `ParamNode` objects as markdown, identifying and linking type names.
    /// @param params a `Reference` object specifying a link
    /// @return Markdown formatted text containing a link
    public static String formatParams(List<ParamNode> params){
        StringBuilder sb = new StringBuilder();
        int paramCount = 0;
        for (ParamNode param : params) {
            if (paramCount++ > 0) sb.append(", ");
            String typeName = formatTypeRef(param.getType(), false);
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
        StringBuilder sb = new StringBuilder();
        switch (typeRef) {
            case TypeReference.Generic generic -> {
                sb.append(link(typeRef.getLink(), useQualifiedName));
                sb.append("<");
                if (generic.hasWildcard()) {
                    sb.append("?");
                } else if (generic.hasExtendsWildcard()) {
                    sb.append("? extends ");
                }
                sb.append(formatTypeRef(generic.getParams(), useQualifiedName));
                sb.append(">");
            }
            case TypeReference.Sequence sequence -> {
                for (TypeReference element : sequence) {
                    if (!sb.isEmpty()) {
                        sb.append(", ");
                    }
                    sb.append(formatTypeRef(element, useQualifiedName));
                }
            }
            case TypeParameter typeParameter -> {
                if (typeParameter.hasExtendsWildcard()) {
                    sb.append("? extends ");
                }
                sb.append(link(typeRef.getLink(), useQualifiedName));
            }
            default -> sb.append(link(typeRef.getLink(), useQualifiedName));
        }
        for (int d = 0; d < typeRef.arrayDimensions(); d++) {
            sb.append("[]");
        }
        return sb.toString();
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

    public static String link(Link reference, boolean useQualifiedName, boolean qualifyMember) {
        return link(reference, null, useQualifiedName, qualifyMember);
    }

    public static String link(Link link, String label, boolean qualifyType, boolean qualifyMember) {
        if (link.getKind() == Link.Kind.METHOD) {
            setLabelForMethod(link, label, qualifyType, qualifyMember);
        } else if(link.getKind() == Link.Kind.URL) {
            setLabelForUrl(link, qualifyType, qualifyMember);
        } else if (link.getKind() == Link.Kind.TYPE) {
            setLabelForType(link, qualifyType);
        }
        if (label!= null && !label.isEmpty()) {
            link.setLabel(label);
        }
        link.setLabel(escape(link.getLabel()));
        return mdRefLink(link);
    }

    private static void setLabelForType(Link link, boolean qualifyType) {
        if (qualifyType) {
            if (!link.getQualifiedClassName().isEmpty()) {
                link.setLabel(link.getQualifiedClassName());
            }
        }else{
            link.setLabel(link.getSimpleClassName());
            if (link.getLabel().isEmpty()) {
                link.setLabel(link.getNestedClassName());
            }
        }
    }

    private static void setLabelForMethod(Link link, String label, boolean qualifyType, boolean qualifyMember) {
        if (qualifyType && !link.getQualifiedClassName().isEmpty()) {
            link.setLabel(link.getQualifiedClassName() + "." + link.getMethodName());
        } else if (qualifyMember && !link.getSimpleClassName().isEmpty()) {
            if (link.getSimpleClassName().isEmpty()) {
                link.setLabel(link.getSimpleClassName() + "." + link.getMethodName());
            } else {
                link.setLabel(link.getNestedClassName() + "." + link.getMethodName());
            }
        } else if (label!= null && !label.isEmpty()) {
            link.setLabel(label);
        }
    }

    private static void setLabelForUrl(Link link, boolean qualifyType, boolean qualifyMember) {
        if (link.getMethodName().isEmpty()) {
            setLabelForUrlWithMethod(link, qualifyType);
        } else {
            if (qualifyMember) {
                if (!link.getSimpleClassName().isEmpty()) {
                    if (link.getSimpleClassName().isEmpty()) {
                        link.setLabel(link.getSimpleClassName() + "." + link.getMethodName());
                    } else {
                        link.setLabel(link.getNestedClassName() + "." + link.getMethodName());
                    }
                }
            } else {
                link.setLabel(link.getMethodName());
            }
        }
    }

    private static void setLabelForUrlWithMethod(Link link, boolean qualifyType) {
        if (qualifyType) {
            if (!link.getQualifiedClassName().isEmpty()) {
                link.setLabel(link.getQualifiedClassName());
            }
        } else {
            if (!link.getSimpleClassName().isEmpty()) {
                link.setLabel(link.getSimpleClassName());
            }
        }
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
            return String.format(FORMAT_SIMPLE_LINK_MD, link.getLabel(), link.getPath());
        } else if (link.getKind() == Link.Kind.PACKAGE) {
            return String.format("[%s](%s/index.md)", link.getLabel(), link.getPath());
        } else if (link.getKind() == Link.Kind.MODULE) {
            return String.format("[%s](%s/index.md)", link.getLabel(), link.getPath());
        } else if (link.getKind() == Link.Kind.URL) {
            if (link.getAnchor().isEmpty()) {
                return String.format(FORMAT_SIMPLE_LINK, link.getLabel(), link.getPath());
            } else {
                return String.format("[%s](%s#%s)", link.getLabel(), link.getPath(), link.getAnchor());
            }
        } else if (link.getKind() == Link.Kind.PAGE) {
            return String.format(FORMAT_SIMPLE_LINK_MD, link.getLabel(), link.getPath());

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
        if (!link.getPath().isEmpty()) {
            return String.format("[%s](%s.md#%s)", displayName, link.getPath(), link.getAnchor());
        } else {
            return String.format("[%s](#%s)", displayName, link.getAnchor());
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
    /// @param anchor A string to be used in an anchor link
    /// @return Markdown formatted text containing a correctly formatted anchor link
    public static String mdAnchorLink(String anchor){
        if (anchor == null || anchor.isEmpty()) return "";
        if (anchor.length() > 1 && anchor.charAt(0) == '#') {
            anchor = anchor.substring(1);
        }
        // Issue: https://github.com/sandydunlop/markista/issues/1
        // Workaround:
        // Remove the parentheses from after method names in anchor
        // links to Markdown pages for now. Anchors in the Markdown
        // are currently headings without parameters.
        return "[" + anchor + "](#" + mdAnchor(anchor) + ")";
    }

    /// Creates a Markdown link to another Markdown document
    /// @param docName The filename of the document being linked to
    /// @return The Markdown formatted link
    public static String mdDocumentLink(String docName) {
        if (docName.contains("://") || docName.endsWith(".md")){
            return String.format(FORMAT_SIMPLE_LINK, docName, docName);
        }
        return String.format(FORMAT_SIMPLE_LINK_MD, docName, docName);
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
