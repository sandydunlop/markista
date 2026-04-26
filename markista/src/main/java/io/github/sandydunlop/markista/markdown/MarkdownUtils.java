package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.core.Context;
import io.github.qishr.cascara.lang.java.model.ParamNode;
import io.github.qishr.cascara.lang.java.model.FileLink;
import io.github.qishr.cascara.lang.java.model.Link;
import io.github.qishr.cascara.lang.java.model.JlsName;
import io.github.qishr.cascara.lang.java.model.Text;
import io.github.qishr.cascara.lang.java.model.VariableTypeNode;
import io.github.qishr.cascara.lang.java.model.VariableTypeNode.BoundingKind;
import io.github.qishr.cascara.lang.java.model.VariableTypeNode.TypeParameter;

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

    /// Create a markdown formatted link
    /// @param link a Link object describing the link
    /// @param useQualifiedName If true, qualified names will be used in the link label
    /// @return markdown formatted link
    public static String link(Link link, boolean useQualifiedName) {
        if (link.getTarget() == null || link.getTarget().getName() == null) {
            if (link instanceof FileLink fileLink) {
                return formatLink(fileLink, fileLink.getLabel());
            } else {
                // Only web links should reach here
                if (link.getUri() == null) {
                    return formatLink(link, "");
                }
                return formatLink(link, link.getUri().toString());
            }
        }
        if (useQualifiedName) {
            return formatLink(link, link.getTarget().getName().fullyQualifiedName());
        } else {
            return formatLink(link, link.getTarget().getName().simpleName());
        }
    }

    /// Formats a list of `ParamNode` objects as markdown, identifying and linking type names.
    /// @param params a `Reference` object specifying a link
    /// @return Markdown formatted text containing a link
    public static String formatParams(List<ParamNode> params){
        StringBuilder sb = new StringBuilder();
        int paramCount = 0;
        for (ParamNode param : params) {
            if (paramCount++ > 0) sb.append(", ");
            String typeName = formatVariableTypeNode(param.getType(), false);
            sb.append(typeName);
            sb.append(" ");
            sb.append(param.getName().simpleName());
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
                    String label = formatLinkLabel(segment, qualifyType, qualifyMember);
                    sb.append(formatLink(segment.getLink(), label));
                    break;
                default:
                    ctx.reportWarning("Unhandled javadoc tag:\n  " + segment.getKind().toString() + "\n  " + segment);
            }
        }
        return sb.toString();
    }

    public static String formatVariableTypeNode(VariableTypeNode typeRef) {
        return formatVariableTypeNode(typeRef, false);
    }

    public static String formatVariableTypeNode(VariableTypeNode variableType, boolean useQualifiedName) {
        StringBuilder sb = new StringBuilder();
        if (variableType.getTypeParameterDeclaration() != null) {
            sb.append(variableType.getTypeParameterDeclaration());
            sb.append(" ");
        }
        switch (variableType) {
            case VariableTypeNode.Generic generic -> {
                sb.append(formatVariableTypeNodeLink(variableType, useQualifiedName));
                sb.append("<");
                sb.append(boundingConstraint(generic));
                sb.append(formatVariableTypeNode(generic.getParams(), useQualifiedName));
                sb.append(">");
            }
            case VariableTypeNode.Sequence sequence -> {
                for (VariableTypeNode element : sequence) {
                    if (!sb.isEmpty()) {
                        sb.append(", ");
                    }
                    sb.append(formatVariableTypeNode(element, useQualifiedName));
                }
            }
            case TypeParameter typeParameter -> {
                sb.append(boundingConstraint(typeParameter));
                sb.append(formatVariableTypeNodeLink(variableType, useQualifiedName));
            }
            default -> sb.append(formatVariableTypeNodeLink(variableType, useQualifiedName));
        }
        for (int d = 0; d < variableType.arrayDimensions(); d++) {
            sb.append("[]");
        }
        return sb.toString();
    }

    private static String boundingConstraint(TypeParameter type) {
        if (type.getBoundingKind() == BoundingKind.UPPER) {
            return type.getBoundingParameter() + " extends ";
        } else if (type.getBoundingKind() == BoundingKind.LOWER) {
            return type.getBoundingParameter() + " super ";
        }
        return "";
    }

    public static String formatVariableTypeNodeLink(VariableTypeNode variableType, boolean useQualifiedName) {
        if (variableType.getLink() != null) {
            return link(variableType.getLink(), useQualifiedName);
        } else {
            return variableType.getRawTypeName();
        }
    }

    /// Formats links as markdown.
    /// @param link The link object containing information about the link
    /// @param label The text that will be displayed for the link
    /// @return Markdown formatted text with a resolved link
    public static String formatLink(Link link, String label) {
        // TODO: Use UriScheme
        if (link.getUri() != null && link.getUri().getScheme() != null && link.getUri().getScheme().startsWith("http")) {
            if (link.getAnchor() != null && !link.getAnchor().isEmpty()) {
                return String.format("[%s](%s#%s)", label, link.getUri(), link.getAnchor());
            } else {
                return String.format(FORMAT_SIMPLE_LINK, label, link.getUri());
            }
        } else {
            return formatLocalLink(link, label);
        }
    }

    public static String formatLocalLink(Link link, String label) {
        if (link.getKind() == Link.Kind.METHOD) {
            return formatLocalMethodLink(link, label);
        } else if (link.getKind() == Link.Kind.TYPE) {
            return String.format(FORMAT_SIMPLE_LINK_MD, label, link.getUri());
        } else if (link.getKind() == Link.Kind.PACKAGE) {
            return String.format("[%s](%sindex.md)", label, link.getUri());
        } else if (link.getKind() == Link.Kind.MODULE) {
            return String.format("[%s](%sindex.md)", label, link.getUri());
        } else if (link.getKind() == Link.Kind.FILE) {
            return String.format(FORMAT_SIMPLE_LINK_MD, label, link.getUri());
        }
        return label;
    }

    /// Creates a markdown formatted link from a [Link] object.
    /// with the option of simplifying qualified type names.
    /// @param link The reference object
    /// @return a markdown formatted link
    public static String formatLocalMethodLink(Link link, String label) {
        if (link.getUri() == null) {
            return String.format("[%s](#%s)", label, link.getAnchor());
        } else {
            return String.format("[%s](%s.md#%s)", label, link.getUri(), link.getAnchor());
        }
    }

    /// Formats links contained in a text segment as markdown.
    /// @param segment A text segment
    /// @return Markdown formatted text with a resolved link
    public static String formatLinkLabel(Text.Segment segment, boolean qualifyType, boolean qualifyMember) {
        Link link = segment.getLink();
        String label = segment.getText();
        if (link.getKind() == Link.Kind.METHOD) {
            label = getLabelForMethod(link, qualifyMember);
        } else if(link.getKind() == Link.Kind.WEB) {
            if (segment.getText().isEmpty()) {
                label = link.getUri().toString();
            } else {
                label = segment.getText();
            }
        } else if (link.getKind() == Link.Kind.TYPE) {
            label = getLabelForType(link, qualifyType);
        }
        return label;
    }

    private static String getLabelForType(Link link, boolean qualifyType) {
        JlsName name = link.getTarget().getName();
        if (qualifyType) {
            return name.fullyQualifiedName();
        } else {
            return name.simpleName();
        }
    }

    private static String getLabelForMethod(Link link, boolean qualifyMember) {
        JlsName name = link.getTarget().getName();
        if (qualifyMember) {
            JlsName typeName = name.typeName();
            JlsName methodName = name.lastComponents(1);
            return typeName.toString() + "." + methodName.toString();
        } else {
            return name.lastComponents(1).toString();
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
    /// @param link MarkdownLink object
    /// @return The Markdown formatted link
    public static String formatFileLink(FileLink link) {
        if (link.getFileName() == null) {
            ctx.reportWarning("No path specified");
            return "";
        } else {
            String uriString = link.getFileName();
            String label = link.getLabel();
            if (uriString.contains("://") || uriString.endsWith(".md")){
                return String.format(FORMAT_SIMPLE_LINK, label, uriString);
            } else {
                return String.format(FORMAT_SIMPLE_LINK_MD, label, uriString);
            }
        }
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
