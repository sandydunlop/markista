package io.github.sandydunlop.markista.doclet;

import io.github.sandydunlop.markista.model.AnnotationNode;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.EnumNode;
import io.github.sandydunlop.markista.model.ExceptionNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.InterfaceNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.Node;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.util.LinkResolver;
import io.github.sandydunlop.markista.util.LinkResolver.Link;
import io.github.sandydunlop.markista.util.LinkResolver.Type;
import io.github.sandydunlop.markista.util.NameUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import javax.print.Doc;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTree.Kind;
import com.sun.source.doctree.SeeTree;

/// A class that outputs API documentation as Markdown.
public class MarkdownWriter {
    private static final String BR = "<br/>";
    private static final String NBSP = "&nbsp;";
    private String outputDirectory;
    private Node linkFrom = null;
    private Writer writer = null;

    /// Constructor that sets up the locations API documents will be written to.
    public MarkdownWriter(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /// Ouput the documentation files for the specified API
    /// @param  api The API to output the documentation for
    public void writeDocs(Api api) throws IOException {
        for (PackageNode packageDoc : api.getPackages()) {
            outputPackageDoc(packageDoc);
        }
    }

    private void outputPackageDoc(PackageNode packageDoc) throws IOException {
        linkFrom = packageDoc; // Used for generating link URLs
        writer = createFile(null, packageDoc.qualifiedName);    
        writer.write("# Package " + packageDoc.qualifiedName + "\n");
        writer.write("\n\n" + formatTaggedText(packageDoc.getFullBody()) + "\n\n");
        outputPackageMembers("Packages", packageDoc.packages);
        outputPackageMembers("Classes", packageDoc.classes);
        outputPackageMembers("Interfaces", packageDoc.interfaces);
        outputPackageMembers("Enum Classes", packageDoc.enumClasses);
        outputPackageMembers("Exception Classes", packageDoc.exceptionClasses);
        outputPackageMembers("Annotation Classes", packageDoc.annotationClasses);
        writer.flush();
        writer.close();
        for (ClassNode doc : packageDoc.classes) {
            outputTypeDoc(doc, "Class");
        }
        for (InterfaceNode doc : packageDoc.interfaces) {
            outputTypeDoc(doc, "Interface");
        }
        for (EnumNode doc : packageDoc.enumClasses) {
            outputTypeDoc(doc, "Enum");
        }
        for (ExceptionNode doc : packageDoc.exceptionClasses) {
            outputTypeDoc(doc, "Exception");
        }
        for (AnnotationNode doc : packageDoc.annotationClasses) {
            outputTypeDoc(doc, "Annotation");
        }
    }

    private void outputPackageMembers(String title, List<?> members) throws IOException {
        if (members.isEmpty()) return;
        String memberKind = "Class";
        if (members.get(0) instanceof PackageNode) {
            memberKind = "Package";
        }
        writer.write("=== \"" + title + "\"\n\n");
        MarkdownTable table = new MarkdownTable()
                .addColumn(memberKind)
                .addColumn("Description");
        for (Node member : (List<Node>)members) {
            table.addRow(new String[]{mdDocumentLink(member.simpleName), inOneLine(formatTaggedText(member.getFirstSentence()))});
        }
        table.render(writer, 4);
    }

    private void outputTypeDoc(TypeNode typeDoc, String subType) throws IOException {
        writer = createFile(typeDoc.simpleName, typeDoc.packageName);    
        writer.write("Package [" + typeDoc.packageName + "](index.md)\n\n");
        writer.write("# " + subType + " " + typeDoc.simpleName + "\n");
        
        outputSupertypes(typeDoc);
        outputImplementedInterfaces(typeDoc);
        //TODO: For interfaces, All Known Implementing Classes
        outputEnclosingClass(typeDoc);
        writer.write("\n----\n\n");

        if (!typeDoc.getBody().isEmpty()) {
            writer.write(formatTaggedText(typeDoc.getBody()));
            writer.write("\n\n");
        }

        if (!typeDoc.classes.isEmpty()) {
            writer.write("\n## Nested Class Summary\n\n");
            outputNestedClassSummary(typeDoc.classes);
        }

        if (typeDoc instanceof EnumNode enumNode && !enumNode.constants.isEmpty()) {
            writer.write("\n##Enum Constants\n\n");
            outputEnumConstantsSummary(enumNode);
        }

        if (!typeDoc.fields.isEmpty()) {
            writer.write("\n## Field Summary\n\n");
            outputFieldSummary(typeDoc.fields);
        }
        if (!typeDoc.constructors.isEmpty()) {
            writer.write("\n## Constructor Summary\n\n");
            outputConstructorSummary(typeDoc.constructors);
        }
        if (!typeDoc.methods.isEmpty()) {
            writer.write("\n## Method Summary\n\n");
            outputMethodSummary(typeDoc.methods);
        }

        if (typeDoc instanceof EnumNode enumNode && !enumNode.constants.isEmpty()) {
            writer.write("\n## Enum Constant Details\n\n");
            outputEnumConstantDetails(enumNode.constants, enumNode);
        }
        if (!typeDoc.methods.isEmpty()) {
            writer.write("\n## Method Details\n\n");
            outputMethodDetails(typeDoc.methods);
        }
        writer.flush();
        writer.close();
        for (ClassNode node : typeDoc.classes) {
            outputTypeDoc(node, "Class");
        }
        for (InterfaceNode node : typeDoc.interfaces) {
            outputTypeDoc(node, "Interface");
        }
        for (EnumNode node : typeDoc.enumClasses) {
            outputTypeDoc(node, "Enum");
        }
        for (ExceptionNode node : typeDoc.exceptionClasses) {
            outputTypeDoc(node, "Exception");
        }
        for (AnnotationNode node : typeDoc.annotationClasses) {
            outputTypeDoc(node, "Annotation");
        }
    }

    private void outputSupertypes(TypeNode typeDoc) throws IOException {
        int indentation = 0;
        for (String st : typeDoc.supertypes) {
            String partiallySimplified = NameUtils.simplifyGenerics(st);
            writer.write(NBSP.repeat(indentation));
            writer.write(mdAutoLink(partiallySimplified, false) + BR + "\n");
            indentation += 8;
        }
        writer.write(NBSP.repeat(indentation));
        writer.write(typeDoc.qualifiedName + BR + "\n");
        writer.write(BR + "\n");
    }

    private void outputImplementedInterfaces(TypeNode typeDoc) throws IOException {
        if (!typeDoc.implementedInterfaces.isEmpty()) {
            writer.write("All Implemented Interfaces:<br/>\n");
            writer.write(NBSP.repeat(4));
            for (int i=0; i<typeDoc.implementedInterfaces.size(); i++) {
                if (i > 0) writer.write(", ");
                writer.write(mdAutoLink(typeDoc.implementedInterfaces.get(i)));
            }
            writer.write("\n\n");
        }
    }

    private void outputEnclosingClass(TypeNode typeDoc) throws IOException {
        if (typeDoc.owner != null && typeDoc.owner instanceof ClassNode) {
            writer.write("Enclosing Class:<br/>\n");
            writer.write(NBSP.repeat(4));
            writer.write(mdAutoLink(typeDoc.owner.simpleName) + "\n\n");
        }
    }

    private void outputNestedClassSummary(List<ClassNode> nestedClasses) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn("Modifier and Type")
                .addColumn("Class")
                .addColumn("Description");
        for (ClassNode nestedClass : nestedClasses) {
            table.addRow(new String[]{nestedClass.getModifiers(),
                        mdDocumentLink(nestedClass.simpleName), 
                        formatTaggedText(nestedClass.getFirstSentence())});
        }
        table.render(writer);
    }

    private void outputEnumConstantsSummary(EnumNode enumNode) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn("Enum Constant")
                .addColumn("Description");
        for (FieldNode constant : enumNode.constants) {
            String link = mdAnchorLink(constant.simpleName);
            table.addRow(new String[]{link, formatTaggedText(constant.getFirstSentence())});
        }
        table.render(writer);
    }

    private void outputFieldSummary(List<FieldNode> fields) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn("Modifier and Type")
                .addColumn("Field")
                .addColumn("Description");
        for (FieldNode fieldDoc : fields) {
            String link = mdAutoLink(fieldDoc.type.qualifiedName, true);
            table.addRow(new String[]{fieldDoc.getModifiers() + link, 
                        fieldDoc.simpleName, formatTaggedText(fieldDoc.getFirstSentence())});
        }
        table.render(writer);
    }

    private void outputConstructorSummary(List<MethodNode> methods) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn("Constructor")
                .addColumn("Description");
        for (MethodNode methodDoc : methods) {
            table.addRow(new String[]{methodDoc.simpleName + "(" + methodDoc.paramsString() + ")",
                        formatTaggedText(methodDoc.getFirstSentence())});
        }
        table.render(writer);
    }

    /// Writes the markdown for a class's method summary table
    /// @param methods The list of methods to include in the summary table
    private void outputMethodSummary(List<MethodNode> methods) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn("Modifier and Type")
                .addColumn("Method")
                .addColumn("Description");
        for (MethodNode methodDoc : methods) {
            table.addRow(new String[]{methodDoc.getModifiers() + 
                        mdAutoLink(methodDoc.returnType.qualifiedName, true), 
                        mdAnchorLink(methodDoc.simpleName) + "(" + methodDoc.paramsString() + ")",
                        formatTaggedText(methodDoc.getFirstSentence())});
        }
        table.render(writer);
    }

    private void outputEnumConstantDetails(List<FieldNode> constants, EnumNode enumNode) throws IOException {
        for (FieldNode constant : constants) {
            writer.write("### " + constant.simpleName + "\n\n");
            writer.write("public static final " + mdAutoLink(enumNode.qualifiedName, true));
            writer.write(" " + constant.fullSignature() + "\n\n");
            writer.write(formatTaggedText(constant.getFullBody()) + "\n\n");

            if (!constant.since.isEmpty()) {
                writer.write("**Since:**\n\n");
                writer.write(formatTaggedText(constant.since.text));
                writer.write("\n\n");
            }

            List<Reference> references = constant.getReferences();
            if (references != null && !references.isEmpty()) {
                writer.write("**See Also:**\n\n");
                for (Reference ref : references) {
                    writer.write("\n");
                    writer.write("See: " + fromatReference(ref) + "\n\n");
                }
                writer.write("\n");
            }
        }
    }

    /// Writes the markdown for a class's method details
    /// @param methods The list of methods to write the details of
    /// @see <a href="http://example.com"/>
    /// @see java.util.List
    /// @since 0.1.0
    private void outputMethodDetails(List<MethodNode> methods) throws IOException {
        for (MethodNode method : methods) {
            writer.write("### " + method.simpleName + "\n\n");
            writer.write("`" + method.fullSignature() + "`\n\n");
            writer.write(formatTaggedText(method.getFullBody()) + "\n\n");

            //TODO: Overrides, Annotations?

            if (!method.params.isEmpty()) {
                boolean showParameters = false;
                for (ParamNode param : method.params) {
                    if (!param.getBody().isEmpty()) showParameters = true; 
                }
                if (showParameters) {
                    writer.write("**Parameters:**\n\n");
                    for (ParamNode param : method.params) {
                        if (!param.getBody().isEmpty()) {
                            writer.write("`" +param.simpleName + "` - " + 
                                    inOneLine(formatTaggedText(param.getBody())) +"\n\n");
                        }
                    }
                }
            }

            if (!isNullOrEmpty(method.returnDescription)) {
                writer.write("**Returns:**\n\n");
                writer.write(method.returnDescription + "\n\n");
            }

            //TODO: Throws


            if (!method.since.isEmpty()) {
                writer.write("**Since:**\n\n");
                writer.write(formatTaggedText(method.since.text));
                writer.write("\n\n");
            }

            List<Reference> references = method.getReferences();
            if (references != null && !references.isEmpty()) {
                writer.write("**See Also:**\n\n");
                for (Reference ref : references) {
                    writer.write("\n");
                    writer.write("See: " + fromatReference(ref) + "\n\n");
                }
                writer.write("\n");
            }
        }
    }

    private String fromatReference(Reference ref) {
        if (ref.kind == Reference.Kind.URL) {
            return mdDocumentLink(ref.url);
        } else if (ref.kind == Reference.Kind.TYPE) {
            return mdAutoLink(ref.typeName);
        }
        return "";
    }

    private String formatTaggedText(List<? extends DocTree> parsedSegments) {
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
            }else{
                System.out.println("Unhandled javadoc tag:");
                System.out.println("  " + segment.getKind().toString());
                System.out.println("  " + segment.toString());
            }
        }
        return sb.toString();
    }

    private String formatTaggedLink(DocTree link) {
        String input = link.toString();
        String[] parts = input.split(" ");
        if (parts.length > 1) {
            parts[1] = parts[1].substring(0, parts[1].length() - 1);
            if (parts[0].equals("{@link")) {
                return mdAutoLink(parts[1]);
            }
        }
        System.out.println("Malformed Javadoc tag: " + link.toString());
        return "";
    }

    private String formatTaggedLinkPlain(DocTree link) {
        String input = link.toString();
        String[] parts = input.split(" ");
        if (parts.length > 2) {
            parts[2] = parts[2].substring(0, parts[2].length() - 1);
            if (parts[0].equals("{@linkplain")) {
                return "[" + mdAutoLink(parts[2]) + "](" + parts[1] + ")";
            }
        }
        System.out.println("Malformed Javadoc tag: " + link.toString());
        return "";
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private static String inOneLine(String text) {
        if (text == null) return "";
        return text.replace("\n", " ");
    }

    private static String mdAnchor(String phrase) {
        return phrase.toLowerCase().replace(" ","");
    }

    private String mdAnchorLink(String phrase){
        return "[" + phrase + "](#" + mdAnchor(phrase) + ")";
    }

    private String mdDocumentLink(String docName) {
        return mdDocumentLink(docName, docName);
    }

    private String mdDocumentLink(String phrase, String docName) {
        if (docName.contains("://") || docName.endsWith(".md")){
            return String.format("[%s](%s)", phrase, docName);
        }
        return String.format("[%s](%s.md)", phrase, docName);
    }

    private String mdAutoLink(String identifier) {
        return mdAutoLink(identifier, true);
    }

    /// Create a markdown link, automatically deciding where it needs to link to
    /// @param identifier a type or package identifier
    /// @param qualify if true, the fully qualified identifier is shown
    /// @return markdown text for a link to a document for the specified identifier or an anchor link
    private String mdAutoLink(String identifier, boolean simplify) {
        Link link = LinkResolver.resolve(linkFrom.qualifiedName, identifier);
        String text;
        if (identifier.indexOf('<') > -1){
            text = escape(NameUtils.simplifyGenerics(identifier));
        } else {
            text = escape(simplify ?  NameUtils.simplifyNames(identifier) : identifier);
        }
        if (link.type == Type.NOTHING) {
            return escape(text);
        }
        if (link.type == Type.CLASS) {
            // ClassNode classDoc = (ClassNode)api.getTypeDoc(identifier, api.getClasses());
            // if (classDoc != null) {
            //     return mdDocumentLink(qualify ? classDoc.qualifiedName : escape(classDoc.simpleName), link.path);
            // }

            // String text = qualify ?  link.path : NameUtils.simplifyNames(link.path);
            return String.format("[%s](%s.md)", text, link.path);
        } else if (link.type == Type.PACKAGE) {
            return String.format("[%s](%s/index.md)", text, link.path);
        } else if (link.type == Type.EXTERNAL) {
            // String text = qualify ?  link.path : NameUtils.simplifyNames(link.path);
            return String.format("[%s](%s)", text, link.path);
        } else {
            // How did we end up here?
            return escape(identifier);
        }
        //     } else {
        //         return mdDocumentLink(qualify ?  identifier : escape(NameUtils.simplifyNames(identifier)), link.path);
        //     }
        // } else {
        //     return escape(qualify ?  identifier : NameUtils.simplifyNames(identifier));
        // }
    }

    private String escape(String str) {
        return str.replace("(","\\(")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /// @param outputDirectory the parsed command line arguments
    /// @param packageName the name of the packageName
    private File buildContainingDirPath(String outputDirectory, String packageName) {
        final File rootDir;
        if (outputDirectory != null) {
            rootDir = new File(outputDirectory);
        } else {
            rootDir = new File(".");
        }

        final String dirName = packageName.replace('.', pathSeparator());
        final File containingDir = new File(rootDir,dirName);
        return containingDir;
    }

    private char pathSeparator() {
        // File.pathSeparatorChar is returnng ":" on macOS (Sequoia 15.5) when it should be "/"
        return File.pathSeparatorChar == ':' ? '/' : File.pathSeparatorChar;
    }

    private Writer createFile(String className, String packageName) throws IOException{
        File containingDir = buildContainingDirPath(outputDirectory, packageName);
        if (!containingDir.exists()) containingDir.mkdirs();
        if (className == null) className = "index";
        File classFile = new File(containingDir, className + ".md");
        FileOutputStream fileOutputStream = new FileOutputStream(classFile);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        return new OutputStreamWriter(bufferedOutputStream);
    }
}