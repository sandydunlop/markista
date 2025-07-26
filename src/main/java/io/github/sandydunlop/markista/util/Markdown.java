package io.github.sandydunlop.markista.util;

import java.util.List;

import javax.tools.Diagnostic;

import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.Text;

public class Markdown {
    private Markdown() {
        // This hides the public constructor
    }

    public static String fullSignature(MethodNode method ) {
        String sig = method.getModifiersString();
        sig += mdAutoLink(method.getReturnType().getQualifiedName(), true) + " ";
        sig += method.getSimpleName() + "(" + formatParams(method.getParams()) + ")";
        return sig;
    }

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

    public static String formatReference(Reference ref) {
        if (ref.getName() == null || ref.getName().isEmpty()) {
            ref.setName(ref.getUri());
        }
        if (ref.getKind() == Reference.Kind.URL) {
            return mdDocumentLink(ref.getUri());
        } else if (ref.getKind() == Reference.Kind.PAGE) {
            String relativePath = LinkResolver.relativize("");
            return mdDocumentLink(ref.getName(), relativePath + ref.getUri());
        } else if (ref.getKind() == Reference.Kind.PACKAGE || ref.getKind() == Reference.Kind.TYPE) {
            return mdAutoLink(ref.getName());
        }
        return "";
    }

    public static String formatText(Text text) {
        List<Text.Segment> parsedSegments = text.getSegments();
        if (parsedSegments == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Text.Segment segment : parsedSegments) {
            switch (segment.getKind()) {
                case Text.SegmentKind.TEXT, Text.SegmentKind.END, Text.SegmentKind.MARKDOWN:
                    sb.append(segment.toString());
                    break;
                case Text.SegmentKind.CODE:
                    sb.append("`");
                    sb.append(segment.getText());
                    sb.append("`");
                    break;
                case Text.SegmentKind.LINK:
                    break;
                case Text.SegmentKind.START:
                    break;
                default:
                    Configuration.getReporter().print(Diagnostic.Kind.WARNING, "Unhandled javadoc tag:");
                    Configuration.getReporter().print(Diagnostic.Kind.WARNING, "  " + segment.getKind().toString());
                    Configuration.getReporter().print(Diagnostic.Kind.WARNING, "  " + segment.toString());
            }
        }
        return sb.toString();
    }

    public static String formatLink(Text.Segment segment) {
        if (segment.getText().isBlank()) {
            return mdAutoLink(segment.getLink());
        } else {
            Reference ref = LinkResolver.resolve(segment.getLink());
            ref.setName(segment.getText());
            return mdRefLink(ref);
        }
    }
   
    public static String mdAutoLink(String identifier) {
        return mdAutoLink(identifier, true);
    }

    /// Create a markdown link, automatically deciding what kind of link to make
    /// @param identifier a package, type, or method identifier
    /// @param simplify if true, the fully simplified version of the identifier is shown
    /// @return markdown text for a link to a document for the specified identifier or an anchor link
    public static String mdAutoLink(String identifier, boolean simplify) {
        boolean isMethod = identifier.indexOf('(') > -1;
        String name = Utils.removeParentheses(identifier);

        String pre = "";
        String text;
        String anchor = "";
        int a = name.indexOf('#');
        if (a > 0) {
            anchor = name.substring(a).toLowerCase();
            name = Utils.removeGenerics(name.substring(0, a));
        }

        if (name == null) {
            return identifier;
        } else if (name.indexOf('<') > -1) {
            text = escape(simplify ? linkGenerics(name, simplify) : name);
            return text;
        } else if (name.indexOf(',') > -1) {
            return splitAndLink(name);
        } else if (name.lastIndexOf(' ') > 0) {
            int p = name.lastIndexOf(' ');
            pre = name.substring(0, p) + " ";
            name = name.substring(p + 1);
        }
        Reference link = LinkResolver.resolve(name);
        text = escape(simplify ?  Utils.simplifyNames(name) : name);
        if (!anchor.isEmpty()) {
            text = anchor.substring(1);
        }
        link.setName(text);
        link.setAnchor(anchor);
        return mdRefLink(pre, link, isMethod);
    }

    public static String mdRefLink(String pre, Reference link, boolean isMethod) {
        if (isMethod) {
            return String.format("%s[%s](#%s)", pre, link.getName(), link.getName());
        } else if (link.getKind() == Reference.Kind.TYPE) {
            return String.format("%s[%s](%s.md%s)", pre, link.getName(), link.getUri(), link.getAnchor());
        } else if (link.getKind() == Reference.Kind.PACKAGE) {
            return String.format("%s[%s](%s/index.md%s)", pre, link.getName(), link.getUri(), link.getAnchor());
        } else if (link.getKind() == Reference.Kind.URL) {
            return String.format("%s[%s](%s%s)", pre, link.getName(), link.getUri(), link.getAnchor());
        }
        return String.format("%s%s", pre, link.getName());
    }

    public static String mdRefLink(Reference link) {
        return mdRefLink("", link, false);
    }
    
    /// Changes qualified generic type names to unqualified generic type names and adds links to their API documentation.
    /// @param str A string containing a qualified generic name.
    /// @return    A string with the qualified names changed to unqualified names and links to types added
    public static String linkGenerics(String str, boolean simplify) {
        if (str == null || str.isEmpty()) return str;
        int start = str.indexOf("<");
        if (start > -1) {
            int end = str.indexOf(">");
            if (end > start) {
                String before = str.substring(0, start);
                before = Markdown.mdAutoLink(before, simplify);
                String after = str.substring(end + 1);
                String mid = str.substring(start + 1, end);
                String simplified = splitAndLink(mid);
                return before + "<" + simplified + ">" + after;
            }
        }
        return mdAutoLink(str, simplify);
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

    public static String mdFolderLink(String docName) {
        return mdFolderLink(docName, docName);
    }

    public static String mdFolderLink(String phrase, String docName) {
        return String.format("[%s](%s)", phrase, docName);
    }

    public static String escape(String str) {
        return str
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
