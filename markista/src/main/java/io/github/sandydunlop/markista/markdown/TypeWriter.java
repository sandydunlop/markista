package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.common.Utils;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.AbstractMember;
import io.github.sandydunlop.markista.model.AnnotationElement;
import io.github.sandydunlop.markista.model.AnnotationTypeNode;
import io.github.sandydunlop.markista.model.AppliedAnnotationNode;
import io.github.sandydunlop.markista.model.ClassTypeNode;
import io.github.sandydunlop.markista.model.Deprecation;
import io.github.sandydunlop.markista.model.EnumTypeNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.Node;
import io.github.sandydunlop.markista.model.NodeKind;
import io.github.sandydunlop.markista.model.Pair;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.model.TypeView;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.InvalidPathException;
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
    public TypeWriter(Context context) {
        ctx = context;
    }

    /// Writes a Markdown file for the Javadoc of a type
    /// @param typeNode the type
    /// @throws java.io.IOException if there is a problem writing to the output file
    public void outputTypeDoc(TypeNode typeNode) throws InvalidPathException, IOException {
        ctx.setTypeName(typeNode.getQualifiedName());
        writer = ctx.createFileInPackage();    
        writer.write("Package [" + typeNode.getPackageName() + "](index.md)\n\n");
        writer.write("# " + typeNode.getKindName() + " " + typeNode.getSimpleName() + "\n");
        
        outputSupertypes(typeNode);
        outputImplementedInterfaces(typeNode);
        outputEnclosingClass(typeNode);
        writer.write("\n----\n\n");

        outputDeclaration(typeNode);

        if (!typeNode.getFullBody().isEmpty()) {
            writer.write(MarkdownUtils.formatText(typeNode.getFullBody()));
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
            outputEnumConstantDetails(enumNode.getConstants());
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
        for (TypeView node : typeNode.getClasses()) {
            outputTypeDoc((TypeNode)node);
        }
        for (TypeView node : typeNode.getInterfaces()) {
            outputTypeDoc((TypeNode)node);
        }
        for (TypeView node : typeNode.getEnums()) {
            outputTypeDoc((TypeNode)node);
        }
        for (TypeView node : typeNode.getAnnotations()) {
            outputTypeDoc((TypeNode)node);
        }
        ctx.setTypeName("");
    }

    /// Outputs the hierarchy of supertypes of a class
    /// @param typeNode the class
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputSupertypes(TypeNode typeNode) throws IOException {
        int indentation = 0;
        for (Pair<Reference,Text> pair : typeNode.getSupertypes()) {
            writer.write(NBSP.repeat(indentation));
            writer.write(MarkdownUtils.formatText(pair.getR(), true) + BR + "\n");
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
                writer.write(MarkdownUtils.link(typeNode.getImplementedInterfaces().get(i), true));
            }
            writer.write("\n\n");
        }
    }

    /// Outputs details of the class that encloses this one
    /// @param typeNode a TypeNode representing the enclosing class
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputEnclosingClass(TypeNode typeNode) throws IOException {
        TypeNode ownerTypeNode = ctx.getApi().getTypeNode(typeNode.getOwner());
        if (ownerTypeNode instanceof ClassTypeNode) {
            writer.write("Enclosing Class:<br/>\n");
            writer.write(NBSP.repeat(4));
            writer.write(MarkdownUtils.link(typeNode.getEnclosingClassRef(), true) + "\n\n");
        }
    }


    /// Outputs the type declaration
    /// @param typeNode the type being documented
    private void outputDeclaration(TypeNode typeNode) throws IOException {
        writer.write("<span style=\"font-family: monospace; font-size: 80%;\">");
        String typeString = typeNode.getKind().toString().toLowerCase();
        if (typeNode.getKind() == NodeKind.ANNOTATION) { 
            typeString = "@interface";
        }
        for (AppliedAnnotationNode annotation : typeNode.getAppliedAnnotations()) {
            if (typeNode.getKind() == NodeKind.ANNOTATION || (annotation.isCustom() && annotation.isDocumented())) {
                writer.write("@" + Utils.simplifyNames(annotation.getTypeName()));
                if (!annotation.getElements().isEmpty()) {
                    writer.write("(");
                    writer.write(createAnnotationString(annotation));
                    writer.write(")");
                }
                writer.write(BR + "\n");
            }
        }
        writer.write(typeNode.getModifiersString() + typeString + " __" + typeNode.getSimpleName() + "__");
        if (typeNode.getSupertypes().size() > 1) {
            Pair<Reference,Text> superTypePair = typeNode.getSupertypes().getLast();
            writer.write(BR);
            writer.write("extends ");
            writer.write(MarkdownUtils.formatText(superTypePair.getR()));
            writer.write("\n");
        }
        writer.write("</span>\n\n");
    }

    /// Outputs the type declaration
    /// @param typeNode the type being documented
    private void outputMethodOrFieldDeclaration(AbstractMember member) throws IOException {
        writer.write("<span style=\"font-family: monospace; font-size: 80%;\">");
        String typeString = "";
        if (member instanceof MethodNode method) {
            typeString = MarkdownUtils.formatText(method.getReturnTypeText());
        } else if (member instanceof FieldNode field) {
            typeString = MarkdownUtils.formatText(field.getTypeText());
        }
        for (AppliedAnnotationNode annotation : member.getAppliedAnnotations()) {
            if (member instanceof AnnotationTypeNode || (annotation.isCustom() && annotation.isDocumented())) {
                writer.write("@" + Utils.simplifyNames(annotation.getTypeName()));
                if (!annotation.getElements().isEmpty()) {
                    writer.write("(");
                    writer.write(createAnnotationString(annotation));
                    writer.write(")");
                }
                writer.write(BR + "\n");
            }
        }
        String modifiers = member.getModifiersString();
        writer.write(modifiers + typeString + " __" + member.getSimpleName() + "__");
        if (member instanceof MethodNode method) {
            writer.write("(");
            writer.write(MarkdownUtils.formatParams(method.getParams()));
            writer.write(")");
        }
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
    private void outputNestedClassSummary(List<TypeView> nestedClasses) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn(TEXT_MODIFIER_AND_TYPE)
                .addColumn(TEXT_CLASS)
                .addColumn(TEXT_DESCRIPTION);
        for (TypeView nestedClassNode : nestedClasses) {
            table.addRow(nestedClassNode.getModifiersString(),
                        MarkdownUtils.mdDocumentLink(nestedClassNode.getSimpleName()), 
                        MarkdownUtils.inOneLine(MarkdownUtils.formatText(nestedClassNode.getFirstSentence())));
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
            String link = MarkdownUtils.mdAnchorLink(constantNode.getSimpleName());
            table.addRow(link, MarkdownUtils.inOneLine(MarkdownUtils.formatText(constantNode.getFirstSentence())));
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
            String link = MarkdownUtils.formatText(fieldNode.getTypeText());
            table.addRow(fieldNode.getModifiersString() + link, 
                        MarkdownUtils.mdAnchorLink(fieldNode.getSimpleName()), MarkdownUtils.inOneLine(MarkdownUtils.formatText(fieldNode.getFirstSentence())));
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
            String params = MarkdownUtils.formatParams(methodNode.getParams());
            table.addRow(methodNode.getSimpleName() + "(" + params + ")",
                        MarkdownUtils.inOneLine(MarkdownUtils.formatText(methodNode.getFirstSentence())));
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
            String returnTypeText = MarkdownUtils.formatText(methodNode.getReturnTypeText());
            table.addRow(methodNode.getModifiersString() + returnTypeText,
                        MarkdownUtils.mdAnchorLink(methodNode.getSimpleName()) + "(" + MarkdownUtils.formatParams(methodNode.getParams()) + ")",
                        MarkdownUtils.inOneLine(MarkdownUtils.formatText(methodNode.getFirstSentence())));
        }
        table.render(writer);
    }

    /// Outputs enum constant details as Markdown
    /// @param constants a list of enum constants
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputEnumConstantDetails(List<FieldNode> constants) throws IOException {
        for (FieldNode constant : constants) {
            writer.write("### " + constant.getSimpleName() + "\n\n");
            writer.write("public static final ");
            writer.write(" " + constant.fullSignature() + "\n\n");
            writer.write(MarkdownUtils.formatText(constant.getFullBody()) + "\n\n");

            if (!constant.getSince().isEmpty()) {
                writer.write("**Since:**\n\n");
                writer.write(MarkdownUtils.formatText(constant.getSince()));
                writer.write("\n\n");
            }

            List<Reference> references = constant.getReferences();
            if (references != null && !references.isEmpty()) {
                writer.write("**See Also:**\n\n");
                for (Reference ref : references) {
                    writer.write("\n");
                    writer.write("See: " + MarkdownUtils.link(ref, false) + "\n\n");
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
                writer.write("### " + method.getSimpleName());
                writer.write("\n\n");
                outputMethodOrFieldDeclaration(method);
            } else if (node instanceof FieldNode field) {
                ctx.setFieldName(field.getSimpleName());
                writer.write("### " + field.getSimpleName());
                writer.write("\n\n");
                outputMethodOrFieldDeclaration(field);
            }
            writer.write(MarkdownUtils.formatText(node.getFullBody()) + "\n\n");

            if (node.getDeprecation() != Deprecation.NONE || !node.getDeprecationText().isEmpty()) {
                outputDeprecation(node.getDeprecation(), node.getDeprecationText());
            }

            if (node instanceof MethodNode method) {
                outputMethodDetails(method);
            }

            if (!node.getSince().isEmpty()) {
                writer.write("**Since:**\n\n");
                writer.write(MarkdownUtils.formatText(node.getSince()));
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
                writer.write(MarkdownUtils.link(ref, false) + "\n\n");
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
            writer.write(MarkdownUtils.formatText(method.getReturnDescription()) + "\n\n");
        }

        if (!method.getThrownTypes().isEmpty()) {
            writer.write("**Throws:**\n\n");
            int count = 0;
            for (Reference thrownType : method.getThrownTypes()) {
                if (count++ > 0) {
                    writer.write(", ");
                }
                writer.write(MarkdownUtils.link(thrownType, false) + "\n");
            }
            writer.write("\n");
        }
        if (method.getSpecifiedBy() != null) {
            writer.write("**Specified By:**\n\n");
            writer.write(MarkdownUtils.link(method.getSpecifiedBy(), false));
            writer.write("\n\n");
        }
        if (method.getBaseMethod() != null) {
            Pair<Reference, Text> pair = method.getBaseMethod();
            if (!pair.getR().isEmpty()) {
                writer.write("**Overrides:**\n\n");
                writer.write(MarkdownUtils.formatText(pair.getR()));
                writer.write("\n\n");
            }
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
                            MarkdownUtils.formatText(param.getFullBody()) +"\n\n");
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
        if (text == null || text.isEmpty()) {
            if (status == Deprecation.FOR_REMOVAL) {
                writer.write("    This has been marked for removal.\n");
            } else {
                writer.write("    This has been marked as deprecated.\n");
            }
        } else {
            writer.write("    " + MarkdownUtils.formatText(text));
        }
        writer.write("\n\n");
    }
}
