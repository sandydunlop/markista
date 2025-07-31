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
            return mdDocumentLink(ref.getName(), FileUtils.joinPaths(relativePath, ref.getUri()));
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
                case Text.SegmentKind.TEXT, Text.SegmentKind.END:
                    sb.append(segment.toString());
                    break;
                case Text.SegmentKind.MARKDOWN:
                    sb.append(formatMarkdown(segment.toString()));
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
                    Configuration.getReporter().print(Diagnostic.Kind.WARNING, "Unhandled javadoc tag:");
                    Configuration.getReporter().print(Diagnostic.Kind.WARNING, "  " + segment.getKind().toString());
                    Configuration.getReporter().print(Diagnostic.Kind.WARNING, "  " + segment.toString());
            }
        }
        return sb.toString();
    }

    public static String formatMarkdown(String markdown) {
        if (markdown.indexOf("[") == -1) {
            return markdown;
        }
        return resolveLinks(markdown);
    }

    public static String formatLink(Text.Segment segment) {
        if (segment.getLink().indexOf("://") > -1) {
            Reference ref = new Reference();
            ref.setKind(Reference.Kind.URL);
            ref.setName(segment.getText());
            ref.setUri(segment.getLink());
            return mdRefLink(ref);
        }
        if (segment.getText().isBlank()) {
            return mdAutoLink(segment.getLink());
        } else {
            Reference ref = LinkResolver.resolve(segment.getLink());
            ref.setName(segment.getText());
            return mdRefLink(ref);
        }
    }

    public static String resolveLinks(String markdown) {
        int openBracket = markdown.length() - 1;
        int openParenthesis = -1;
        int closeBracket = -1;
        int closeParenthesis = -1;
        while (openBracket > 0) {
            char c = markdown.charAt(openBracket);
            if (c == ')') {
                closeParenthesis = openBracket;
                openParenthesis = -1;
            } else if (c == ']') {
                closeBracket = openBracket;
            } else if (c == '(' && closeParenthesis > -1) {
                openParenthesis = openBracket;
                closeBracket = -1;
            } else if (c == '[' && closeBracket > -1) {
                markdown = processMarkdownLink(markdown, openBracket, closeBracket, openParenthesis, closeParenthesis);
            }
            openBracket--;
        }
        return markdown;
    }

    private static String processMarkdownLink(String markdown, int openBracket, int closeBracket, int openParenthesis, int closeParenthesis) {
        String before = markdown.substring(0, openBracket);
        String text = markdown.substring(openBracket + 1, closeBracket);
        Reference link;
        String after = "";
        if (openParenthesis > -1) {
            // Brackets with link in parentheses
            after = markdown.substring(closeParenthesis + 1);
            String path = markdown.substring(openParenthesis + 1, closeParenthesis);
            int pos = path.indexOf('#');
            String anchor = "";
            if (pos > 0) {
                anchor = path.substring(pos).toLowerCase();
                path = Utils.removeGenerics(path.substring(0, pos));
            }
            link = LinkResolver.resolve(path);
            link.setAnchor(anchor);
        } else {
            // Brackets without parentheses
            after = markdown.substring(closeBracket + 1);
            link = LinkResolver.resolve(text);
        }
        link.setName(text);
        return mdRefLink(before, link, after);
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
        String post = "";
        String anchor = "";
        String text;
        int pos = name.indexOf('#');
        if (pos > 0) {
            anchor = name.substring(pos);
            name = Utils.removeGenerics(name.substring(0, pos));
        }
        if (name == null) {
            return identifier;
        } else if (name.indexOf('<') > -1) {
            text = escape(simplify ? linkGenerics(name) : name);
            return text;
        } else if (name.indexOf(',') > -1) {
            return splitAndLink(name);
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
        Reference link = LinkResolver.resolve(name);
        if (simplify) {
            link.setName(Utils.simplifyNames(link.getName()));
        }
        link.setName(escape(link.getName()));
        link.setAnchor(anchor);
        if (isMethod) {
            link.setKind(Reference.Kind.METHOD);
        }
        return mdRefLink(pre, link, post);
    }

    public static String mdRefLink(String pre, Reference link, String post) {
        String name;
        if (link.getAnchor().isEmpty()) {
            name = link.getName();
        } else {
            name = link.getName() + "." + link.getAnchor().substring(1);
        }
        link.setAnchor(link.getAnchor().toLowerCase());
        if (link.getKind() == Reference.Kind.METHOD) {
            return String.format("%s[%s](%s%s)%s", pre, name, link.getUri(), link.getAnchor(), post);
        } else if (link.getKind() == Reference.Kind.TYPE) {
            return String.format("%s[%s](%s.md%s)%s", pre, name, link.getUri(), link.getAnchor(), post);
        } else if (link.getKind() == Reference.Kind.PACKAGE) {
            return String.format("%s[%s](%s/index.md%s)%s", pre, name, link.getUri(), link.getAnchor(), post);
        } else if (link.getKind() == Reference.Kind.MODULE) {
            return String.format("%s[%s](%s/index.md)%s", pre, name, link.getUri(), post);
        } else if (link.getKind() == Reference.Kind.URL) {
            return String.format("%s[%s](%s%s)%s", pre, name, link.getUri(), link.getAnchor(), post);
        }
        return String.format("%s%s%s", pre, link.getName(), post);
    }

    public static String mdRefLink(Reference link) {
        return mdRefLink("", link, "");
    }
    
    /// Changes qualified generic type names to unqualified generic type names and adds links to their API documentation.
    /// @param str A string containing a qualified generic name.
    /// @return    A string with the qualified names changed to unqualified names and links to types added
    public static String linkGenerics(String str) {
        if (str == null || str.isEmpty()) return str;
        int openingChevron = str.indexOf("<");
        int closingChevron = str.lastIndexOf(">");
        String before = str.substring(0, openingChevron);
        String mid = str.substring(openingChevron + 1, closingChevron);
        String after = str.substring(closingChevron + 1);
        before = mdAutoLink(before);
        mid = splitAndLink(mid);
        return before + "&lt;" + mid + "&gt;" + after;
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
