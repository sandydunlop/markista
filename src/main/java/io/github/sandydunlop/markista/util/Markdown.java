package io.github.sandydunlop.markista.util;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTree.Kind;
import com.sun.source.doctree.StartElementTree;

import java.util.List;

import javax.tools.Diagnostic;
import javax.lang.model.element.Name;

import io.github.sandydunlop.markista.doclet.Configuration;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.Text;

public class Markdown {
    private static final String TEXT_MALFORMED_TAG = "Malformed Javadoc tag: ";
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

    public static String formatTaggedText(Text text) {
        List<? extends DocTree> parsedSegments = text.getSegments();
        if (parsedSegments == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (DocTree segment : parsedSegments) {
            if (segment.getKind() == Kind.MARKDOWN) {
                sb.append(segment.toString());
            } else if (segment.getKind() == Kind.LINK) {
                String link = formatTaggedLink(segment);
                sb.append(link);
            } else if (segment.getKind() == Kind.LINK_PLAIN) {
                String link = formatTaggedLinkPlain(segment);
                sb.append(link);
            } else if (segment.getKind() == Kind.CODE) {
                sb.append(formatTaggedCode(segment));
            } else if (segment.getKind() == Kind.TEXT) {
                sb.append(segment.toString());
            } else if (segment.getKind() == Kind.START_ELEMENT) {
                StartElementTree se = (StartElementTree)segment;
                Name name = se.getName();
                if ("p".equals(name.toString())) {
                    sb.append("\n\n");
                }
            } else if (segment.getKind() == Kind.END_ELEMENT) {
                sb.append(segment.toString());
            } else {
                Configuration.getReporter().print(Diagnostic.Kind.WARNING, "Unhandled javadoc tag:");
                Configuration.getReporter().print(Diagnostic.Kind.WARNING, "  " + segment.getKind().toString());
                Configuration.getReporter().print(Diagnostic.Kind.WARNING, "  " + segment.toString());
            }
        }
        return sb.toString();
    }

    public static String formatTaggedCode(DocTree code) {
        String input = code.toString();
        String[] parts = input.split(" ");
        if (parts.length > 1 && parts[0].equals("{@code")) {
            parts[parts.length-1] = parts[parts.length-1].substring(0, parts[parts.length-1].length() - 1);
            StringBuilder sb = new StringBuilder();
            sb.append("`");
            for (int i = 1; i<parts.length; i++) {
                if (i > 1) {
                    sb.append(" ");
                }
                sb.append(parts[i]);
            }
            sb.append("`");
            return sb.toString();
        }
        Configuration.getReporter().print(Diagnostic.Kind.WARNING, TEXT_MALFORMED_TAG + code.toString());
        return "";
    }

    public static String formatTaggedLink(DocTree link) {
        String input = link.toString();
        String[] parts = input.split(" ");
        if (parts.length > 1) {
            parts[parts.length-1] = parts[parts.length-1].substring(0, parts[parts.length-1].length() - 1);
            if (parts[0].equals("{@link")) {
                return mdAutoLink(parts[1]);
            }
        }
        Configuration.getReporter().print(Diagnostic.Kind.WARNING, TEXT_MALFORMED_TAG + link.toString());
        return "";
    }

    public static String formatTaggedLinkPlain(DocTree link) {
        String input = link.toString();
        String[] parts = input.split(" ");
        if (parts.length > 2) {
            parts[parts.length-1] = parts[parts.length-1].substring(0, parts[parts.length-1].length() - 1);
            if (parts[0].equals("{@linkplain")) {
                return "[" + mdAutoLink(parts[2]) + "](" + parts[1] + ")";
            }
        }
        Configuration.getReporter().print(Diagnostic.Kind.WARNING, TEXT_MALFORMED_TAG + link.toString());
        return "";
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
        String name = Util.removeParentheses(identifier);

        String pre = "";
        String text;
        String anchor = "";
        int a = name.indexOf('#');
        if (a > 0) {
            anchor = name.substring(a).toLowerCase();
            name = Util.removeGenerics(name.substring(0, a));
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
        text = escape(simplify ?  Util.simplifyNames(name) : name);
        if (!anchor.isEmpty()) {
            text = anchor.substring(1);
        }
        if (isMethod) {
            return String.format("%s[%s](#%s)", pre, text, text);
        } else if (link.getKind() == Reference.Kind.TYPE) {
            return String.format("%s[%s](%s.md%s)", pre, text, link.getUri(), anchor);
        } else if (link.getKind() == Reference.Kind.PACKAGE) {
            return String.format("%s[%s](%s/index.md%s)", pre, text, link.getUri(), anchor);
        } else if (link.getKind() == Reference.Kind.URL) {
            return String.format("%s[%s](%s%s)", pre, text, link.getUri(), anchor);
        }
        return String.format("%s%s", pre, text);
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
                before = Markdown.mdAutoLink(before, simplifiy);
                String after = str.substring(end + 1);
                String mid = str.substring(start + 1, end);
                String simplified = splitAndLink(mid);
                return before + "<" + simplified + ">" + after;
            }
        }
        return mdAutoLink(str, simplifiy);
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
