package io.github.sandydunlop.markista.doclet;

import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.Deprecation;
import io.github.sandydunlop.markista.model.EnumNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.Node;
import io.github.sandydunlop.markista.model.PackageMember;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.util.LinkResolver;
import io.github.sandydunlop.markista.util.Markdown;
import io.github.sandydunlop.markista.util.Files;
import io.github.sandydunlop.markista.util.Util;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.type.TypeMirror;

/// A class that outputs API documentation as Markdown.
public class PackageWriter {
    private static final String TEXT_CLASS = "Class";
    private static final String TEXT_DESCRIPTION = "Description";
    private static final String TEXT_MODIFIER_AND_TYPE = "Modifier and Type";
    private static final String BR = "<br/>";
    private static final String NBSP = "&nbsp;";

    private String outputDirectory;
    private Writer writer = null;
    private Files fileUtils;

    /// Constructor that sets up the locations API documents will be written to.
    public PackageWriter(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /// Ouput the documentation files for the specified API
    /// @param moduleNode  The module containing the packages to output the documentation for
    public void writeDocs(ModuleNode moduleNode) throws IOException {
        fileUtils = new Files(moduleNode, outputDirectory);
        for (PackageMember node : moduleNode.getPackages()) {
            if (node instanceof PackageNode packageNode) {
                // outputConstantValues(moduleNode);
                outputPackageDoc(packageNode);
            }
        }
    }

    private void outputPackageDoc(PackageNode packageNode) throws IOException {
        LinkResolver.setLocation(packageNode.getName()); // Used for generating link URLs
        writer = fileUtils.createFile(null, packageNode.getName());    
        writer.write("# Package " + packageNode.getName() + "\n");
        writer.write("\n\n" + Markdown.formatText(packageNode.getFullBody()) + "\n\n");
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
            table.addRow(Markdown.mdDocumentLink(member.getName()), Util.inOneLine(Markdown.formatText(member.getDescription())));
        }
        table.render(writer, 4);
    }

    private void outputTypeDoc(TypeNode typeNode) throws IOException {
        outputTypeDoc(typeNode, typeNode.getKind().toString());
    }

    private void outputTypeDoc(TypeNode typeNode, String typeKind) throws IOException {
        writer = fileUtils.createFile(typeNode.getSimpleName(), typeNode.getPackageName());    
        writer.write("Package [" + typeNode.getPackageName() + "](index.md)\n\n");
        writer.write("# " + typeKind + " " + typeNode.getSimpleName() + "\n");
        
        outputSupertypes(typeNode);
        outputImplementedInterfaces(typeNode);
        outputEnclosingClass(typeNode);
        writer.write("\n----\n\n");

        if (!typeNode.getBody().isEmpty()) {
            writer.write(Markdown.formatText(typeNode.getBody()));
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
            writer.write(Markdown.mdAutoLink(st, false) + BR + "\n");
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
                writer.write(Markdown.mdAutoLink(typeDoc.getImplementedInterfaces().get(i)));
            }
            writer.write("\n\n");
        }
    }

    private void outputEnclosingClass(TypeNode typeDoc) throws IOException {
        if (typeDoc.getOwner() instanceof ClassNode) {
            writer.write("Enclosing Class:<br/>\n");
            writer.write(NBSP.repeat(4));
            writer.write(Markdown.mdAutoLink(typeDoc.getOwner().getName()) + "\n\n");
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
                        Markdown.mdDocumentLink(nestedClassNode.getSimpleName()), 
                        Markdown.formatText(nestedClassNode.getFirstSentence()));
        }
        table.render(writer);
    }

    private void outputEnumConstantsSummary(EnumNode enumNode) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn("Enum Constant")
                .addColumn(TEXT_DESCRIPTION);
        for (FieldNode constantNode : enumNode.getConstants()) {
            String link = Markdown.mdAnchorLink(constantNode.getSimpleName());
            table.addRow(link, Markdown.formatText(constantNode.getFirstSentence()));
        }
        table.render(writer);
    }

    private void outputFieldSummary(List<FieldNode> fields) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn(TEXT_MODIFIER_AND_TYPE)
                .addColumn("Field")
                .addColumn(TEXT_DESCRIPTION);
        for (FieldNode fieldNode : fields) {
            String link = Markdown.mdAutoLink(fieldNode.getType().getQualifiedName(), true);
            table.addRow(fieldNode.getModifiersString() + link, 
                        Markdown.mdAnchorLink(fieldNode.getSimpleName()), Markdown.formatText(fieldNode.getFirstSentence()));
        }
        table.render(writer);
    }

    private void outputConstructorSummary(List<MethodNode> methods) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn("Constructor")
                .addColumn(TEXT_DESCRIPTION);
        for (MethodNode methodNode : methods) {
            table.addRow(methodNode.getSimpleName() + "(" + Markdown.formatParams(methodNode.getParams()) + ")",
                        Markdown.formatText(methodNode.getFirstSentence()));
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
                        Markdown.mdAutoLink(methodNode.getReturnType().getQualifiedName(), true), 
                        Markdown.mdAnchorLink(methodNode.getSimpleName()) + "(" + Markdown.formatParams(methodNode.getParams()) + ")",
                        Util.inOneLine(Markdown.formatText(methodNode.getFirstSentence())));
        }
        table.render(writer);
    }

    private void outputEnumConstantDetails(List<FieldNode> constants, EnumNode enumNode) throws IOException {
        for (FieldNode constant : constants) {
            writer.write("### " + constant.getSimpleName() + "\n\n");
            writer.write("public static final " + Markdown.mdAutoLink(enumNode.getQualifiedName(), true));
            writer.write(" " + constant.fullSignature() + "\n\n");
            writer.write(Markdown.formatText(constant.getFullBody()) + "\n\n");

            if (!constant.getSince().isEmpty()) {
                writer.write("**Since:**\n\n");
                writer.write(Markdown.formatText(constant.getSince()));
                writer.write("\n\n");
            }

            List<Reference> references = constant.getReferences();
            if (references != null && !references.isEmpty()) {
                writer.write("**See Also:**\n\n");
                for (Reference ref : references) {
                    writer.write("\n");
                    writer.write("See: " + Markdown.formatReference(ref) + "\n\n");
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
                writer.write(Markdown.fullSignature(method) + "\n\n");
            } else if (node instanceof FieldNode field) {
                writer.write("### " + field.getSimpleName() + "\n\n");
            }
            writer.write(Markdown.formatText(node.getFullBody()) + "\n\n");

            if (node.getDeprecation() != Deprecation.NONE || !node.getDeprecationText().isEmpty()) {
                outputDeprecation(node.getDeprecation(), node.getDeprecationText());
            }

            if (node instanceof MethodNode method) {
                outputMethodDetails(method);
            }

            if (!node.getSince().isEmpty()) {
                writer.write("**Since:**\n\n");
                writer.write(Markdown.formatText(node.getSince()));
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
                writer.write(Markdown.formatReference(ref) + "\n\n");
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
            writer.write(Markdown.formatText(method.getReturnDescription()) + "\n\n");
        }

        if (!method.getThrownTypes().isEmpty()) {
            writer.write("**Throws:**\n\n");
            int count = 0;
            for (TypeMirror typeMirror : method.getThrownTypes()) {
                if (count++ > 0) {
                    writer.write(", ");
                }
                String qualifiedNAme = typeMirror.toString();

                writer.write(Markdown.mdAutoLink(qualifiedNAme, true) + "\n");
            }
            writer.write("\n");
        }
        if (!method.getSpecifiedBy().isEmpty()) {
            writer.write("**Specified By:**\n\n");
            writer.write(Markdown.mdAutoLink(method.getSpecifiedBy(), false));
            writer.write("\n\n");
        }
        if (method.getOverriddenMethod() != null && !method.getOverriddenMethod().getClassName().isEmpty() && !method.getOverriddenMethod().getMethodName().isEmpty()) {
            writer.write("**Overrides:**\n\n");
            writer.write(Markdown.mdAutoLink(method.getOverriddenMethod().getClassName() + "#" + method.getOverriddenMethod().getMethodName()) + " from " + Markdown.mdAutoLink(method.getOverriddenMethod().getClassName()));
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
                            Util.inOneLine(Markdown.formatText(param.getBody())) +"\n\n");
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
            writer.write("    " + Markdown.formatText(text));
        }
        writer.write("\n\n");
    }
}