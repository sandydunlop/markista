package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.model.AnnotationElement;
import io.github.sandydunlop.markista.model.AppliedAnnotationNode;
import io.github.sandydunlop.markista.model.ClassTypeNode;
import io.github.sandydunlop.markista.model.Deprecation;
import io.github.sandydunlop.markista.model.EnumTypeNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.Node;
import io.github.sandydunlop.markista.model.PackageMember;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.util.Context;
import io.github.sandydunlop.markista.util.LinkResolver;
import io.github.sandydunlop.markista.util.Markdown;
import io.github.sandydunlop.markista.util.Utils;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/// A class that outputs API type documentation as Markdown.
public class TypeWriter {
    private static final String TEXT_CLASS = "Class";
    private static final String TEXT_DESCRIPTION = "Description";
    private static final String TEXT_MODIFIER_AND_TYPE = "Modifier and Type";
    private static final String BR = "<br/>";
    private static final String NBSP = Character.toString(0x00A0);

    /// The Context singleton instance providing access to the current documentation generation context,
    /// including configuration, current module/package/type names, and reporting utilities.
    /// > **Warning**<br/>
    /// Do not make this `final`. It will break tests with mocked [Context].
    private Context ctx;

    /// The Writer used to output the generated markdown content for the current document.
    /// It handles writing text to the appropriate output file or stream.
    private Writer writer = null;

    /// Constructor that sets up the locations API documents will be written to.
    public TypeWriter() {
        ctx = Context.getInstance();
    }

    /// Tells the [LinkResolver] what class is about to be documented then 
    /// calls the appropriate method do begin the documentation process.
    /// @param typeNode The type to be documented
    /// @throws java.io.IOException if there is a problem writing to the output file
    public void writeDoc(TypeNode typeNode) throws IOException {
        outputTypeDoc(typeNode, typeNode.getKind().toString());
    }

    /// Writes a Markdown file for the Javadoc of a type
    /// @param typeNode the type
    /// @param typeKind The name of the kind of the type eg. "Class", or "Enum"
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputTypeDoc(TypeNode typeNode, String typeKind) throws IOException {
        ctx.setTypeName(typeNode.getQualifiedName());
        writer = ctx.createFileInPackage();    
        writer.write("Package [" + typeNode.getPackageName() + "](index.md)\n\n");
        writer.write("# " + typeKind + " " + typeNode.getSimpleName() + "\n");
        
        outputSupertypes(typeNode);
        outputImplementedInterfaces(typeNode);
        outputEnclosingClass(typeNode);
        writer.write("\n----\n\n");

        outputDeclaration(typeNode);

        if (!typeNode.getFullBody().isEmpty()) {
            writer.write(Markdown.formatText(typeNode.getFullBody()));
            writer.write("\n\n");
        }

        if (!typeNode.getClasses().isEmpty()) {
            writer.write("\n## Nested Class Summary\n\n");
            outputNestedClassSummary(typeNode.getClasses());
        }

        if (typeNode instanceof EnumTypeNode enumNode && !enumNode.getConstants().isEmpty()) {
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
        if (typeNode instanceof EnumTypeNode enumNode && !enumNode.getConstants().isEmpty()) {
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
            writeDoc((TypeNode)node);
        }
        for (PackageMember node : typeNode.getInterfaces()) {
            writeDoc((TypeNode)node);
        }
        for (PackageMember node : typeNode.getEnums()) {
            writeDoc((TypeNode)node);
        }
        for (PackageMember node : typeNode.getAnnotations()) {
            writeDoc((TypeNode)node);
        }
        ctx.setTypeName("");
    }

    /// Outputs the hierarchy of supertypes of a class
    /// @param typeNode the class
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputSupertypes(TypeNode typeNode) throws IOException {
        int indentation = 0;
        for (String st : typeNode.getSupertypes()) {
            writer.write(NBSP.repeat(indentation));
            writer.write(Markdown.link(Reference.to(st), true) + BR + "\n");
            indentation += 8;
        }
        writer.write(NBSP.repeat(indentation));
        writer.write(typeNode.getQualifiedName() + BR + "\n");
        writer.write(BR + "\n");
    }

    /// Outputs details of any interfaces that this class implements
    /// @param typeNode a TypeNode representing a class, interface, enum, or annotation
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputImplementedInterfaces(TypeNode typeNode) throws IOException {
        if (!typeNode.getImplementedInterfaces().isEmpty()) {
            writer.write("All Implemented Interfaces:<br/>\n");
            writer.write(NBSP.repeat(4));
            for (int i=0; i<typeNode.getImplementedInterfaces().size(); i++) {
                if (i > 0) writer.write(", ");
                writer.write(Markdown.link(Reference.to(typeNode.getImplementedInterfaces().get(i)), true));
            }
            writer.write("\n\n");
        }
    }

    /// Outputs details of the class that encloses this one
    /// @param enclosingClass a TypeNode representing the enclosing class
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputEnclosingClass(TypeNode enclosingClass) throws IOException {
        if (enclosingClass.getOwner() instanceof ClassTypeNode) {
            writer.write("Enclosing Class:<br/>\n");
            writer.write(NBSP.repeat(4));
            writer.write(Markdown.link(Reference.to(enclosingClass.getOwner().getName()), true) + "\n\n");
        }
    }


    /// Outputs the type declaration
    /// @param typeNode the type being documented
    private void outputDeclaration(TypeNode typeNode) throws IOException {
        writer.write("<span style=\"font-family: monospace;\">");
        String typeString = typeNode.getKind().toString();
        if (typeNode.getKind() == TypeNode.Kind.ANNOTATION) { 
            typeString = "@interface";
        }
        for (AppliedAnnotationNode annotation : typeNode.getAppliedAnnotations()) {
            if (typeNode.getKind() == TypeNode.Kind.ANNOTATION || (annotation.isCustom() && annotation.isDocumented())) {
                writer.write("@" + annotation.getType().getSimpleName());
                if (!annotation.getElements().isEmpty()) {
                    writer.write("(");
                    writer.write(createAnnotationString(annotation));
                    writer.write(")");
                }
                writer.write(BR + "\n");
            }
        }
        writer.write(typeNode.getModifiersString() + typeString + " __" + typeNode.getSimpleName() + "__");
        writer.write("</span>\n\n");
    }

    /// Creates a string of the elements within an annotation
    /// @param annotation the annotation
    /// @return The string
    private String createAnnotationString(AppliedAnnotationNode annotation) {
        StringBuilder sb = new StringBuilder();
        for (AnnotationElement element : annotation.getElements()) {
            if (!sb.isEmpty()) sb.append(", ");
            if (annotation.getElements().size() > 1) {
                sb.append(element.getSimpleName());
                sb.append(" ");
            }
            sb.append(element.getValue());
        }
        return sb.toString();
    }
    /// Outputs a summary of nested classes within this one as Markdown
    /// @param nestedClasses a list of the nested classes
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputNestedClassSummary(List<PackageMember> nestedClasses) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn(TEXT_MODIFIER_AND_TYPE)
                .addColumn(TEXT_CLASS)
                .addColumn(TEXT_DESCRIPTION);
        for (PackageMember member : nestedClasses) {
            if (!(member instanceof TypeNode nestedClassNode)) continue;
            table.addRow(nestedClassNode.getModifiersString(),
                        Markdown.mdDocumentLink(nestedClassNode.getSimpleName()), 
                        Utils.inOneLine(Markdown.formatText(nestedClassNode.getFirstSentence())));
        }
        table.render(writer);
    }

    /// Outputs as summary of an enum's constants as Markdown
    /// @param enumNode the enum
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputEnumConstantsSummary(EnumTypeNode enumNode) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn("Enum Constant")
                .addColumn(TEXT_DESCRIPTION);
        for (FieldNode constantNode : enumNode.getConstants()) {
            String link = Markdown.mdAnchorLink(constantNode.getSimpleName());
            table.addRow(link, Utils.inOneLine(Markdown.formatText(constantNode.getFirstSentence())));
        }
        table.render(writer);
    }

    /// Outputs a summary of this class's fields as Markdown
    /// @param fields A list of fields belonging to this class
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputFieldSummary(List<FieldNode> fields) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn(TEXT_MODIFIER_AND_TYPE)
                .addColumn("Field")
                .addColumn(TEXT_DESCRIPTION);
        for (FieldNode fieldNode : fields) {
            String link = Markdown.link(Reference.to(fieldNode.getType().getQualifiedName()), false);
            table.addRow(fieldNode.getModifiersString() + link, 
                        Markdown.mdAnchorLink(fieldNode.getSimpleName()), Utils.inOneLine(Markdown.formatText(fieldNode.getFirstSentence())));
        }
        table.render(writer);
    }

    /// Outputs a the summary of this class's constructors as Markdown
    /// @param methods A list of the constructor methods
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputConstructorSummary(List<MethodNode> methods) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn("Constructor")
                .addColumn(TEXT_DESCRIPTION);
        for (MethodNode methodNode : methods) {
            table.addRow(methodNode.getSimpleName() + "(" + Markdown.formatParams(methodNode.getParams()) + ")",
                        Utils.inOneLine(Markdown.formatText(methodNode.getFirstSentence())));
        }
        table.render(writer);
    }

    /// Writes the markdown for a class's method summary table
    /// @param methods The list of methods to include in the summary table
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputMethodSummary(List<MethodNode> methods) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn(TEXT_MODIFIER_AND_TYPE)
                .addColumn("Method")
                .addColumn(TEXT_DESCRIPTION);
        for (MethodNode methodNode : methods) {
            table.addRow(methodNode.getModifiersString() + 
                        Markdown.link(Reference.to(methodNode.getReturnType().getQualifiedName()), false), 
                        Markdown.mdAnchorLink(methodNode.getSimpleName()) + "(" + Markdown.formatParams(methodNode.getParams()) + ")",
                        Utils.inOneLine(Markdown.formatText(methodNode.getFirstSentence())));
        }
        table.render(writer);
    }

    /// Outputs enum constant details as Markdown
    /// @param constants a list of enum constants
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputEnumConstantDetails(List<FieldNode> constants, EnumTypeNode enumNode) throws IOException {
        for (FieldNode constant : constants) {
            writer.write("### " + constant.getSimpleName() + "\n\n");
            writer.write("public static final " + Markdown.link(Reference.to(enumNode.getQualifiedName()), true));
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
            writer.write("\n---\n\n");
        }
    }

    /// Writes the markdown for a class's method or field details.
    /// @param nodes The list of methods to write the details of
    /// @see <a href="http://example.com"/>
    /// @see java.util.List
    /// @since 0.1.0
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputDetails(List<Node> nodes) throws IOException {
        for (Node node : nodes) {
            if (node instanceof MethodNode method) {
                ctx.setMethodName(method.getSimpleName());
                writer.write("### " + method.getSimpleName() + "\n\n");
                writer.write(Markdown.fullSignature(method) + "\n\n");
            } else if (node instanceof FieldNode field) {
                ctx.setFieldName(field.getSimpleName());
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
            writer.write("\n---\n\n");
            ctx.setFieldName("");
            ctx.setMethodName("");
        }
    }

    /// Outputs references for a type member
    /// @param node The type
    /// @throws java.io.IOException if there is a problem writing to the output file
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

    /// Outputs the Javadoc of a method as Markdown
    /// @param method the method
    /// @throws java.io.IOException if there is a problem writing to the output file
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
            for (String thrownType : method.getThrownTypes()) {
                if (count++ > 0) {
                    writer.write(", ");
                }
                writer.write(Markdown.link(Reference.to(thrownType), false) + "\n");
            }
            writer.write("\n");
        }
        if (!method.getSpecifiedBy().isEmpty()) {
            writer.write("**Specified By:**\n\n");
            writer.write(Markdown.link(Reference.to(method.getSpecifiedBy()), false));
            writer.write("\n\n");
        }
        if (method.getOverriddenMethod() != null && !method.getOverriddenMethod().getClassName().isEmpty() && !method.getOverriddenMethod().getMethodName().isEmpty()) {
            writer.write("**Overrides:**\n\n");
            writer.write(Markdown.link(Reference.to(method.getOverriddenMethod().getClassName() + "#" + method.getOverriddenMethod().getMethodName()), false) + " from " + Markdown.link(Reference.to(method.getOverriddenMethod().getClassName()), false));
            writer.write("\n\n");
        }        
    }

    /// Outputs the parameters of a method as Markdown
    /// @param method the method
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputMethodParams(MethodNode method) throws IOException {
        boolean showParameters = false;
        for (ParamNode param : method.getParams()) {
            if (!param.getFullBody().isEmpty()) showParameters = true; 
        }
        if (showParameters) {
            writer.write("**Parameters:**\n\n");
            for (ParamNode param : method.getParams()) {
                if (!param.getFullBody().isEmpty()) {
                    writer.write("`" +param.getSimpleName() + "` - " + 
                            Markdown.formatText(param.getFullBody()) +"\n\n");
                }
            }
        }
    }

    /// Outputs the deprecation status of this type as Markdown
    /// @param status The deprecation status
    /// @param text A textual description of the deprecation status
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputDeprecation(Deprecation status, Text text) throws IOException {
        writer.write("\n\n");
        writer.write("!!! note \"Deprecation\"\n");
        if (Utils.isNullOrEmpty(text)) {
            if (status == Deprecation.FOR_REMOVAL) {
                writer.write("    This has been marked for removal.\n");
            } else {
                writer.write("    This has been marked as deprecated.\n");
            }
        } else {
            writer.write("    " + Markdown.formatText(text));
        }
        writer.write("\n\n");
    }
}
