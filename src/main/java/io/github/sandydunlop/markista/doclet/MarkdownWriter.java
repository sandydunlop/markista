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
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.util.LinkResolver;
import io.github.sandydunlop.markista.util.NameUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

/// A class that outputs API documentation.
public class MarkdownWriter {
    private static final String BR = "<br/>";
    private static final String NBSP = "&nbsp;";
    private Api api = null;
    private String outputDirectory;
    private TypeNode linkFrom = null;
    private Writer writer = null;

    /// Constructor that sets up the locations API documents will be written to.
    public MarkdownWriter(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /// Ouput the documentation files for the specified API
    /// @param  api The API to output the documentation for
    public void writeDocs(Api api) throws IOException {
        this.api = api;
        for (PackageNode packageDoc : api.getPackages()) {
            outputPackageDoc(packageDoc);
        }
    }

    private void outputPackageDoc(PackageNode packageDoc) throws IOException {
        writer = createFile(null, packageDoc.qualifiedName);    
        writer.write("# Package " + packageDoc.qualifiedName + "\n");
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
        writer.write("=== \"" + title + "\"\n\n");
        MarkdownTable table = new MarkdownTable()
                .addColumn("Class")
                .addColumn("Description");
        for (Node member : (List<Node>)members) {
            table.addRow(new String[]{mdDocumentLink(member.simpleName), inOneLine(member.description)});
        }
        table.render(writer, 4);
    }

    private void outputTypeDoc(TypeNode typeDoc, String subType) throws IOException {
        linkFrom = typeDoc; // Used for generating link URLs
        writer = createFile(typeDoc.simpleName, typeDoc.packageName);    
        writer.write("Package [" + typeDoc.packageName + "](index.md)\n\n");
        writer.write("# " + subType + " " + typeDoc.simpleName + "\n");
        
        outputSupertypes(typeDoc);
        //TODO: Type parameters eg  for Class Enum<E extends Enum<E>> explain E
        outputImplementedInterfaces(typeDoc);
        //TODO: For interfaces, All Known Implementing Classes
        outputEnclosingClass(typeDoc);
        writer.write("\n----\n\n");

        if (!isNullOrEmpty(typeDoc.fullDescription)) {
            writer.write(typeDoc.fullDescription);
            writer.write("\n\n");
        }

        if (!typeDoc.classes.isEmpty()) {
            writer.write("\n## Nested Class Summary\n\n");
            outputNestedClassSummary(typeDoc.classes);
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
        if (!typeDoc.methods.isEmpty()) {
            writer.write("\n## Method Details\n\n");
            outputMethodDetails(typeDoc.methods);
        }
        writer.flush();
        writer.close();
        for (ClassNode nestedClass : typeDoc.classes) {
            outputTypeDoc(nestedClass, "Class");
        }
    }

    private void outputSupertypes(TypeNode typeDoc) throws IOException {
        int indentation = 0;
        for (String st : typeDoc.supertypes) {
            String partiallySimplified = NameUtils.simplifyGenerics(st);
            writer.write(NBSP.repeat(indentation));
            writer.write(mdAutoLink(partiallySimplified, true) + BR + "\n");
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
                        nestedClass.description});
        }
        table.render(writer);
    }

    private void outputFieldSummary(List<FieldNode> fields) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn("Modifier and Type")
                .addColumn("Field")
                .addColumn("Description");
        for (FieldNode fieldDoc : fields) {
            String link = mdAutoLink(fieldDoc.type.qualifiedName, false);
            table.addRow(new String[]{fieldDoc.getModifiers() + link, 
                        fieldDoc.simpleName, fieldDoc.description});
        }
        table.render(writer);
    }

    private void outputConstructorSummary(List<MethodNode> methods) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn("Constructor")
                .addColumn("Description");
        for (MethodNode methodDoc : methods) {
            table.addRow(new String[]{methodDoc.simpleName + "(" + methodDoc.paramsString() + ")",
                        methodDoc.description});
        }
        table.render(writer);
    }

    private void outputMethodSummary(List<MethodNode> methods) throws IOException {
        MarkdownTable table = new MarkdownTable()
                .addColumn("Modifier and Type")
                .addColumn("Method")
                .addColumn("Description");
        for (MethodNode methodDoc : methods) {
            table.addRow(new String[]{methodDoc.getModifiers() + mdAutoLink(methodDoc.returnType.qualifiedName, false), 
                        mdAnchorLink(methodDoc.simpleName) + "(" + methodDoc.paramsString() + ")",
                        methodDoc.description});
        }
        table.render(writer);
    }

    private void outputMethodDetails(List<MethodNode> methods) throws IOException {
        for (MethodNode methodDoc : methods) {
            writer.write("### " + methodDoc.simpleName + "\n\n");
            writer.write("`" + methodDoc.fullSignature() + "`\n\n");
            writer.write(methodDoc.fullDescription + "\n\n");

            //TODO: Overrides, exceptions. Annotations?

            if (!methodDoc.params.isEmpty()) {
                boolean showParameters = false;
                for (ParamNode param : methodDoc.params) {
                    if (!isNullOrEmpty(param.description)) showParameters = true; 
                }
                if (showParameters) {
                    writer.write("Parameters:\n\n");
                    for (ParamNode param : methodDoc.params) {
                        if (!isNullOrEmpty(param.description)) {
                            writer.write("`" +param.simpleName + "` - " + inOneLine(param.description) +"\n\n");
                        }
                    }
                }
            }

            if (!isNullOrEmpty(methodDoc.returnDescription)) {
                writer.write("Returns:\n\n");
                writer.write(methodDoc.returnDescription + "\n\n");
            }
            
            // writer.write("#### See Also:\n\n");
        }
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
        return mdAutoLink(identifier, false);
    }

    /// Create a markdown link, automatically deciding where it needs to link to
    /// @param identifier a type or package identifier
    /// @param qualify if true, the fully qualified identifier is shown
    /// @return markdown text for a link to a document for the specified identifier or an anchor link
    private String mdAutoLink(String identifier, boolean qualify) {
        String link = LinkResolver.resolve(linkFrom.qualifiedName, identifier);
        if (link != null) {
            ClassNode classDoc = (ClassNode)api.getTypeDoc(identifier, api.getClasses());
            if (classDoc != null) {
                return mdDocumentLink(qualify ? classDoc.qualifiedName : escape(classDoc.simpleName), link);
            } else {
                return mdDocumentLink(qualify ?  identifier : escape(NameUtils.simplifyNames(identifier)), link);
            }
        } else {
            return escape(qualify ?  identifier : NameUtils.simplifyNames(identifier));
        }
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