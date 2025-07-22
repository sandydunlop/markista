package io.github.sandydunlop.markista.doclet;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTree.Kind;
import com.sun.source.doctree.StartElementTree;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.Deprecation;
import io.github.sandydunlop.markista.model.EnumNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.Node;
import io.github.sandydunlop.markista.model.PackageMember;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.util.LinkResolver;
import io.github.sandydunlop.markista.util.Util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;

/// A class that outputs API documentation as Markdown.
public class MarkdownWriter {
    private static final String TEXT_CLASS = "Class";
    private static final String TEXT_DESCRIPTION = "Description";
    private static final String TEXT_MODIFIER_AND_TYPE = "Modifier and Type";
    private static final String BR = "<br/>";
    private static final String NBSP = "&nbsp;";

    private boolean squashEmptyDirectories = false;
    private String squashedDirectories = null;
    private String outputDirectory;
    private Writer writer = null;

    /// Constructor that sets up the locations API documents will be written to.
    public MarkdownWriter(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void setSquashEmptyDirectories(boolean b) {
        squashEmptyDirectories = b;
    }

    /// Ouput the documentation files for the specified API
    /// @param  api The API to output the documentation for
    public void writeDocs(Api api) throws IOException {
        setSquashedDirectories(api);
        for (PackageNode packageNode : api.getPackages()) {
            outputPackageDoc(packageNode);
        }
        outputConstantValues(api);
    }

    private void setSquashedDirectories(Api api) {
        squashedDirectories = null;
        int dotCount = 0;
        if (squashEmptyDirectories) {
            for (PackageNode packageNode : api.getPackages()) {
                if (squashedDirectories == null) {
                    squashedDirectories = packageNode.getName();
                    dotCount = countDots(squashedDirectories);
                } else if (countDots(packageNode.getName()) < dotCount) {
                    dotCount = countDots(packageNode.getName());
                    squashedDirectories = packageNode.getName();
                }
            }
        }
        if (squashedDirectories != null && squashedDirectories.lastIndexOf('.') > -1) {
            squashedDirectories = squashedDirectories.substring(0, squashedDirectories.lastIndexOf('.'));
            LinkResolver.setSquashedDirectories(squashedDirectories);
        }
    }

    private static int countDots(String str) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '.') {
                count++;
            }
        }
        return count;
    }

    private void outputPackageDoc(PackageNode packageNode) throws IOException {
        LinkResolver.setLocation(packageNode.getName()); // Used for generating link URLs
        writer = createFile(null, packageNode.getName());    
        writer.write("# Package " + packageNode.getName() + "\n");
        writer.write("\n\n" + formatTaggedText(packageNode.getFullBody()) + "\n\n");
        outputPackageMembers("Packages", packageNode.getPackages());
        outputPackageMembers("Classes", packageNode.getClasses());
        outputPackageMembers("Interfaces", packageNode.getInterfaces());
        outputPackageMembers("Enum Classes", packageNode.getEnums());
        outputPackageMembers("Annotation Classes", packageNode.getAnnotations());
        writer.flush();
        writer.close();
        for (PackageMember member : packageNode.getClasses()) {
            outputTypeDoc((TypeNode)member);
        }
        for (PackageMember member : packageNode.getInterfaces()) {
            outputTypeDoc((TypeNode)member);
        }
        for (PackageMember member : packageNode.getEnums()) {
            outputTypeDoc((TypeNode)member);
        }
        for (PackageMember member : packageNode.getAnnotations()) {
            outputTypeDoc((TypeNode)member);
        }
    }

    private void outputPackageMembers(String title, List<PackageMember> members) throws IOException {
        if (members.isEmpty()) return;
        String memberKind = TEXT_CLASS;
        if (members.get(0) instanceof PackageNode) {
            memberKind = "Package";
        }
        writer.write("=== \"" + title + "\"\n\n");
        MarkdownTable table = new MarkdownTable()
                .addColumn(memberKind)
                .addColumn(TEXT_DESCRIPTION);
        for (PackageMember member : members) {
            table.addRow(Util.mdDocumentLink(member.getName()), Util.inOneLine(formatTaggedText(member.getDescription())));
        }
        table.render(writer, 4);
    }

    private void outputTypeDoc(TypeNode typeNode) throws IOException {
        outputTypeDoc(typeNode, typeNode.getKind().toString());
    }

    private void outputTypeDoc(TypeNode typeNode, String typeKind) throws IOException {
        writer = createFile(typeNode.getSimpleName(), typeNode.getPackageName());    
        writer.write("Package [" + typeNode.getPackageName() + "](index.md)\n\n");
        writer.write("# " + typeKind + " " + typeNode.getSimpleName() + "\n");
        
        outputSupertypes(typeNode);
        outputImplementedInterfaces(typeNode);
        outputEnclosingClass(typeNode);
        writer.write("\n----\n\n");

        if (!typeNode.getBody().isEmpty()) {
            writer.write(formatTaggedText(typeNode.getBody()));
            writer.write("\n\n");
        }

        if (!typeNode.getClasses().isEmpty()) {
            writer.write("\n## Nested Class Summary\n\n");
            outputNestedClassSummary(typeNode.getClasses());
        }

        if (typeNode instanceof EnumNode enumNode && !enumNode.getConstants().isEmpty()) {
            writer.write("\n##Enum Constants\n\n");
            outputEnumConstantsSummary(enumNode);
        }

        if (!typeNode.getFields().isEmpty()) {
            writer.write("\n## Field Summary\n\n");
            outputFieldSummary(typeNode.getFields());
        }
        if (!typeNode.getConstructors().isEmpty()) {
            writer.write("\n## Constructor Summary\n\n");
            outputConstructorSummary(typeNode.getConstructors());
        }
        if (!typeNode.getMethods().isEmpty()) {
            writer.write("\n## Method Summary\n\n");
            outputMethodSummary(typeNode.getMethods());
        }
        if (typeNode instanceof EnumNode enumNode && !enumNode.getConstants().isEmpty()) {
            writer.write("\n## Enum Constant Details\n\n");
            outputEnumConstantDetails(enumNode.getConstants(), enumNode);
        }
        if (!typeNode.getFields().isEmpty()) {
            writer.write("\n## Field Details\n\n");
            outputDetails(new ArrayList<>(typeNode.getFields()));
        }
        if (!typeNode.getMethods().isEmpty()) {
            writer.write("\n## Method Details\n\n");
            outputDetails(new ArrayList<>(typeNode.getMethods()));
        }
        writer.flush();
        writer.close();
        for (PackageMember node : typeNode.getClasses()) {
            outputTypeDoc((TypeNode)node);
        }
        for (PackageMember node : typeNode.getInterfaces()) {
            outputTypeDoc((TypeNode)node);
        }
        for (PackageMember node : typeNode.getEnums()) {
            outputTypeDoc((TypeNode)node);
        }
        for (PackageMember node : typeNode.getAnnotations()) {
            outputTypeDoc((TypeNode)node);
        }
    }

    private void outputSupertypes(TypeNode typeDoc) throws IOException {
        int indentation = 0;
        for (String st : typeDoc.getSupertypes()) {
            writer.write(NBSP.repeat(indentation));
            writer.write(Util.mdAutoLink(st, false) + BR + "\n");
            indentation += 8;
        }
        writer.write(NBSP.repeat(indentation));
        writer.write(typeDoc.getQualifiedName() + BR + "\n");
        writer.write(BR + "\n");
    }

    private void outputImplementedInterfaces(TypeNode typeDoc) throws IOException {
        if (!typeDoc.getImplementedInterfaces().isEmpty()) {
            writer.write("All Implemented Interfaces:<br/>\n");
            writer.write(NBSP.repeat(4));
            for (int i=0; i<typeDoc.getImplementedInterfaces().size(); i++) {
                if (i > 0) writer.write(", ");
                writer.write(Util.mdAutoLink(typeDoc.getImplementedInterfaces().get(i)));
            }
            writer.write("\n\n");
        }
    }

    private void outputEnclosingClass(TypeNode typeDoc) throws IOException {
        if (typeDoc.getOwner() instanceof ClassNode) {
            writer.write("Enclosing Class:<br/>\n");
            writer.write(NBSP.repeat(4));
            writer.write(Util.mdAutoLink(typeDoc.getOwner().getName()) + "\n\n");
        }
    }

    private void outputNestedClassSummary(List<PackageMember> nestedClasses) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn(TEXT_MODIFIER_AND_TYPE)
                .addColumn(TEXT_CLASS)
                .addColumn(TEXT_DESCRIPTION);
        for (PackageMember member : nestedClasses) {
            if (!(member instanceof TypeNode nestedClassNode)) continue;
            table.addRow(nestedClassNode.getModifiersString(),
                        Util.mdDocumentLink(nestedClassNode.getSimpleName()), 
                        formatTaggedText(nestedClassNode.getFirstSentence()));
        }
        table.render(writer);
    }

    private void outputEnumConstantsSummary(EnumNode enumNode) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn("Enum Constant")
                .addColumn(TEXT_DESCRIPTION);
        for (FieldNode constantNode : enumNode.getConstants()) {
            String link = Util.mdAnchorLink(constantNode.getSimpleName());
            table.addRow(link, formatTaggedText(constantNode.getFirstSentence()));
        }
        table.render(writer);
    }

    private void outputFieldSummary(List<FieldNode> fields) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn(TEXT_MODIFIER_AND_TYPE)
                .addColumn("Field")
                .addColumn(TEXT_DESCRIPTION);
        for (FieldNode fieldNode : fields) {
            String link = Util.mdAutoLink(fieldNode.getType().getQualifiedName(), true);
            table.addRow(fieldNode.getModifiersString() + link, 
                        Util.mdAnchorLink(fieldNode.getSimpleName()), formatTaggedText(fieldNode.getFirstSentence()));
        }
        table.render(writer);
    }

    private void outputConstructorSummary(List<MethodNode> methods) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn("Constructor")
                .addColumn(TEXT_DESCRIPTION);
        for (MethodNode methodNode : methods) {
            table.addRow(methodNode.getSimpleName() + "(" + paramsString(methodNode.getParams()) + ")",
                        formatTaggedText(methodNode.getFirstSentence()));
        }
        table.render(writer);
    }

    /// Writes the markdown for a class's method summary table
    /// @param methods The list of methods to include in the summary table
    private void outputMethodSummary(List<MethodNode> methods) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn(TEXT_MODIFIER_AND_TYPE)
                .addColumn("Method")
                .addColumn(TEXT_DESCRIPTION);
        for (MethodNode methodNode : methods) {
            table.addRow(methodNode.getModifiersString() + 
                        Util.mdAutoLink(methodNode.getReturnType().getQualifiedName(), true), 
                        Util.mdAnchorLink(methodNode.getSimpleName()) + "(" + paramsString(methodNode.getParams()) + ")",
                        Util.inOneLine(formatTaggedText(methodNode.getFirstSentence())));
        }
        table.render(writer);
    }

    private void outputEnumConstantDetails(List<FieldNode> constants, EnumNode enumNode) throws IOException {
        for (FieldNode constant : constants) {
            writer.write("### " + constant.getSimpleName() + "\n\n");
            writer.write("public static final " + Util.mdAutoLink(enumNode.getQualifiedName(), true));
            writer.write(" " + constant.fullSignature() + "\n\n");
            writer.write(formatTaggedText(constant.getFullBody()) + "\n\n");

            if (!constant.getSince().isEmpty()) {
                writer.write("**Since:**\n\n");
                writer.write(formatTaggedText(constant.getSince()));
                writer.write("\n\n");
            }

            List<Reference> references = constant.getReferences();
            if (references != null && !references.isEmpty()) {
                writer.write("**See Also:**\n\n");
                for (Reference ref : references) {
                    writer.write("\n");
                    writer.write("See: " + formatReference(ref) + "\n\n");
                }
                writer.write("\n");
            }
        }
    }

    /// Writes the markdown for a class's method or field details.
    /// @param nodes The list of methods to write the details of
    /// @see <a href="http://example.com"/>
    /// @see java.util.List
    /// @since 0.1.0
    private void outputDetails(List<Node> nodes) throws IOException {
        for (Node node : nodes) {
            if (node instanceof MethodNode method) {
                writer.write("### " + method.getSimpleName() + "\n\n");
                writer.write(fullSignature(method) + "\n\n");
            } else if (node instanceof FieldNode field) {
                writer.write("### " + field.getSimpleName() + "\n\n");
            }
            writer.write(formatTaggedText(node.getFullBody()) + "\n\n");

            if (node.getDeprecation() != Deprecation.NONE || !node.getDeprecationText().isEmpty()) {
                outputDeprecation(node.getDeprecation(), node.getDeprecationText());
            }

            if (node instanceof MethodNode method) {
                outputMethodDetails(method);
            }

            if (!node.getSince().isEmpty()) {
                writer.write("**Since:**\n\n");
                writer.write(formatTaggedText(node.getSince()));
                writer.write("\n\n");
            }

            outputReferences(node);
        }
    }

    private void outputReferences(Node node) throws IOException {
        if (!node.getReferences().isEmpty()) {
            writer.write("**See Also:**\n\n");
            for (Reference ref : node.getReferences()) {
                writer.write("\n");
                writer.write(formatReference(ref) + "\n\n");
            }
            writer.write("\n");
        }
    }

    private void outputMethodDetails(MethodNode method) throws IOException {
        if (!method.getParams().isEmpty()) {
            outputMethodParams(method);
        }

        if (!method.getReturnDescription().isEmpty()) {
            writer.write("**Returns:**\n\n");
            writer.write(formatTaggedText(method.getReturnDescription()) + "\n\n");
        }

        if (!method.getThrownTypes().isEmpty()) {
            writer.write("**Throws:**\n\n");
            int count = 0;
            for (TypeMirror typeMirror : method.getThrownTypes()) {
                if (count++ > 0) {
                    writer.write(", ");
                }
                String qualifiedNAme = typeMirror.toString();

                writer.write(Util.mdAutoLink(qualifiedNAme, true) + "\n");
            }
            writer.write("\n");
        }
        if (!method.getSpecifiedBy().isEmpty()) {
            writer.write("**Specified By:**\n\n");
            writer.write(Util.mdAutoLink(method.getSpecifiedBy(), false));
            writer.write("\n\n");
        }
        if (method.getOverriddenMethod() != null && !method.getOverriddenMethod().getClassName().isEmpty() && !method.getOverriddenMethod().getMethodName().isEmpty()) {
            writer.write("**Overrides:**\n\n");
            writer.write(Util.mdAutoLink(method.getOverriddenMethod().getClassName() + "#" + method.getOverriddenMethod().getMethodName()) + " from " + Util.mdAutoLink(method.getOverriddenMethod().getClassName()));
            writer.write("\n\n");
        }        
    }

    private void outputMethodParams(MethodNode method) throws IOException {
        boolean showParameters = false;
        for (ParamNode param : method.getParams()) {
            if (!param.getBody().isEmpty()) showParameters = true; 
        }
        if (showParameters) {
            writer.write("**Parameters:**\n\n");
            for (ParamNode param : method.getParams()) {
                if (!param.getBody().isEmpty()) {
                    writer.write("`" +param.getSimpleName() + "` - " + 
                            Util.inOneLine(formatTaggedText(param.getBody())) +"\n\n");
                }
            }
        }
    }

    private void outputDeprecation(Deprecation status, Text text) throws IOException {
        writer.write("\n\n");
        writer.write("!!! note \"Deprecated\"\n");
        if (text.isEmpty()) {
            writer.write("    This has been marked as deprecated.\n");
        } else {
            writer.write("    " + formatTaggedText(text));
        }
        writer.write("\n\n");
    }

    private void outputConstantValues(Api api) throws IOException {
        writer = createFile("constant-values", "");    
        if (!api.getConstantValues().isEmpty()) {
            writer.write("# Constant Field Values\n");
            MarkdownTable table = new MarkdownTable()
                    .addColumn(TEXT_MODIFIER_AND_TYPE)
                    .addColumn("Constant Field")
                    .addColumn("Value");
            for (FieldNode constantValue : api.getConstantValues()) {
                StringBuilder modifiersAndType = new StringBuilder();
                if (constantValue.getModifiersString().isEmpty()) {
                    modifiersAndType.append(constantValue.getModifiersString());
                    modifiersAndType.append(" ");
                }
                modifiersAndType.append(Util.mdAutoLink(constantValue.getType().getQualifiedName(), true));
                table.addRow(modifiersAndType.toString(), constantValue.getSimpleName(), constantValue.getConstantValue().toString());
            }
            table.render(writer);
        }

        writer.flush();
        writer.close();

    }

    public String fullSignature(MethodNode method ) {
        String sig = method.getModifiersString();
        sig += Util.mdAutoLink(method.getReturnType().getQualifiedName(), true) + " ";
        sig += method.getSimpleName() + "(" + paramsString(method.getParams()) + ")";
        return sig;
    }

    public String paramsString(List<ParamNode> params){
        String str = "";
        int paramCount = 0;
        for (ParamNode param : params) {
            if (paramCount++ > 0) str += ", ";
            String typeName = Util.mdAutoLink(param.getType().getQualifiedName(), true);
            str+=typeName + param.getType().getArrayBrackets() + " " + param.getSimpleName(); 
        }
        return str;
    }

    private String formatReference(Reference ref) {
        if (ref.getKind() == Reference.Kind.URL) {
            return Util.mdDocumentLink(ref.getUri());
        } else if (ref.getKind() == Reference.Kind.PAGE) {
            String relativePath = LinkResolver.relativize("");
            return Util.mdDocumentLink(ref.getName(), relativePath + ref.getUri());
        } else if (ref.getKind() == Reference.Kind.PACKAGE || ref.getKind() == Reference.Kind.TYPE) {
            return Util.mdAutoLink(ref.getName());
        }
        return "";
    }

    private String formatTaggedText(Text text) {
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
                System.out.println("Unhandled javadoc tag:");
                System.out.println("  " + segment.getKind().toString());
                System.out.println("  " + segment.toString());
            }
        }
        return sb.toString();
    }

    private String formatTaggedCode(DocTree code) {
        String input = code.toString();
        String[] parts = input.split(" ");
        if (parts.length > 1) {
            if (parts[0].equals("{@code")) {
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
                return "`" + parts[1] + "`";
            }
        }
        System.out.println("Malformed Javadoc tag: " + code.toString());
        return "";
    }

    private String formatTaggedLink(DocTree link) {
        String input = link.toString();
        String[] parts = input.split(" ");
        if (parts.length > 1) {
            parts[parts.length-1] = parts[parts.length-1].substring(0, parts[parts.length-1].length() - 1);
            if (parts[0].equals("{@link")) {
                return Util.mdAutoLink(parts[1]);
            }
        }
        System.out.println("Malformed Javadoc tag: " + link.toString());
        return "";
    }

    private String formatTaggedLinkPlain(DocTree link) {
        String input = link.toString();
        String[] parts = input.split(" ");
        if (parts.length > 2) {
            parts[parts.length-1] = parts[parts.length-1].substring(0, parts[parts.length-1].length() - 1);
            if (parts[0].equals("{@linkplain")) {
                return "[" + Util.mdAutoLink(parts[2]) + "](" + parts[1] + ")";
            }
        }
        System.out.println("Malformed Javadoc tag: " + link.toString());
        return "";
    }

    /// @param outputDirectory output path specified by the `-d` command line parameter
    /// @param packageName the name of the packageName
    private File buildContainingDirPath(String outputDirectory, String packageName) {
        final File rootDir;
        if (outputDirectory != null) {
            rootDir = new File(outputDirectory);
        } else {
            rootDir = new File(".");
        }

        String dirName = packageName.replace('.', pathSeparator());
        if (squashEmptyDirectories && dirName.length() > 2) {
           dirName = dirName.substring(squashedDirectories.length());
        }
        return new File(rootDir, dirName);
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