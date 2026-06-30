package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.core.Context;
import io.github.qishr.cascara.lang.java.model.SemanticModel;
import io.github.qishr.cascara.lang.java.model.DirectiveNode;
import io.github.qishr.cascara.lang.java.model.FieldNode;
import io.github.qishr.cascara.lang.java.model.FileLink;
import io.github.qishr.cascara.lang.java.model.JlsName;
import io.github.qishr.cascara.lang.java.model.ModuleNode;
import io.github.qishr.cascara.lang.java.model.PackageNode;
import io.github.qishr.cascara.lang.java.model.Link;
import io.github.qishr.cascara.lang.java.model.TypeNode;
import io.github.qishr.cascara.lang.java.model.VariableTypeNode;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

/// A class that outputs a module's API documentation as Markdown.
public class ModuleWriter {
    private static final String TITLE_CONSTANT_FIELD = "Constant Field";
    private static final String TITLE_CONSTANT_FIELD_VALUES = "Constant Field Values";
    private static final String TITLE_DESCRIPTION = "Description";
    private static final String TITLE_EXPORTS = "Exports";
    private static final String TITLE_IMPLEMENTATIONS = "Implementations";
    private static final String TITLE_INTERFACE = "Interface";
    private static final String TITLE_MODIFIER_AND_TYPE = "Modifier and Type";
    private static final String TITLE_MODULE = "Module";
    private static final String TITLE_OPENS = "Opens";
    private static final String TITLE_PACKAGE = "Package";
    private static final String TITLE_PACKAGES = "Packages";
    private static final String TITLE_PROVIDES = "Provides";
    private static final String TITLE_REQUIRES = "Requires";
    private static final String TITLE_USES = "Uses";
    private static final String TITLE_VALUE = "Value";
    private static final String NL = "\n";

    /// The Context singleton instance providing access to the current documentation generation context,
    /// including configuration, current module/package/type names, and reporting utilities.
    /// > **Warning**<br/>
    /// Do not make this `final`. It will break tests with mocked [Context].
    private Context ctx;

    /// The Writer used to output the generated markdown content for the current document.
    /// It handles writing text to the appropriate output file or stream.
    private Writer writer;

    /// The SemanticModel model representing the entire documented API structure,
    /// including modules, packages, types, and members used for cross-referencing and navigation.
    private SemanticModel api;

    /// Constructor that sets up the locations API documents will be written to.
    public ModuleWriter(Context context) {
        this.ctx = context;
    }

    /// Output the documentation files for the specified API
    /// @param  api The API to output the documentation for
    /// @throws java.io.IOException if there is a problem writing to the output file
    public void writeDocs(SemanticModel api) throws InvalidPathException, IOException {
        this.api = api;
        for (ModuleNode moduleNode : api.getModules()) {
            outputModuleDoc(moduleNode);
        }
        outputModuleDoc(api.getUnnamedModuleNode());
        outputSummary();
        outputIndex();
    }

    private void outputIndex()  throws InvalidPathException, IOException {
        ctx.setModuleName(null);
        ctx.setPackageName(null);
        writer = ctx.createFileInRoot("index.md");

        // writer.write("""
        //     ---
        //     hide:
        //       - navigation
        //       - toc
        //     ---
        //     """);

        writer.write("# Cascara Javadoc Overview" + NL);

        MarkdownTable table = new MarkdownTable()
            .addColumn(TITLE_MODULE)
            .addColumn(TITLE_DESCRIPTION);

        for (ModuleNode moduleNode : api.getModules()) {
            String description = MarkdownUtils.formatText(moduleNode.getFirstSentence());

            String moduleName = moduleNode.getName().fullyQualifiedName();
            String linkString = "[" + moduleNode.getName().fullyQualifiedName() + "](" + moduleName + "/index.md)";

            table.addRow(linkString, description);
        }

        table.render(writer, 0);
        writer.flush();
        writer.close();
    }

    private void outputSummary()  throws InvalidPathException, IOException {
        ctx.setModuleName(null);
        ctx.setPackageName(null);
        writer = ctx.createFileInRoot("SUMMARY.md");
        writer.write("* [Overview](index.md)" + NL);
        for (ModuleNode moduleNode : api.getModules()) {
            ctx.setModuleName(moduleNode.getName().fullyQualifiedName());
            String moduleName = moduleNode.getName().fullyQualifiedName();
            writer.write("* [" + moduleNode.getName().fullyQualifiedName() + "](" + moduleName + "/index.md)");
            writer.write(NL);

            for (DirectiveNode directive : moduleNode.getExports()) {
                Link link = directive.getLink();
                JlsName packageName = link.getTarget().getName();
                PackageNode packageNode = api.getPackageNode(packageName);

                // String docPathString = link.getTarget().
                // FileLink typeLink = FileLink.to(docPathString).withLabel(typeNode.getName().simpleName());
                // String linkString = MarkdownUtils.formatFileLink(typeLink);

                String uriString = moduleName + "/" + link.getUri().toString();
                link.setUri(URI.create(uriString));


                String base = ctx.getCommonBasePath();
                String linkText = packageName.fullyQualifiedName().substring(base.length() + 1);

                // String linkText = packageName.simpleName();
                String linkString = MarkdownUtils.formatLink(link, linkText);
                writer.write("    * " + linkString + NL);

                ctx.setPackageName(packageName.fullyQualifiedName());
                File packageDir = ctx.getPackageDirectory();
                Path packageDirPath = packageDir.toPath();

                Path rootPath = Path.of(ctx.getOutputDirectory());
                Path relPath = rootPath.relativize(packageDirPath);

                for (TypeNode typeNode : packageNode.getTypes()) {
                    Path docPath = relPath.resolve(typeNode.getName().simpleName());
                    String docPathString = docPath.toString();
                    FileLink typeLink = FileLink.to(docPathString).withLabel(typeNode.getName().simpleName());
                    linkString = MarkdownUtils.formatFileLink(typeLink);
                    writer.write("        * " + linkString + NL);
                }
            }
        }
        writer.flush();
        writer.close();
    }

    /// Outputs a single module's documentation as a Markdown file.
    /// For each module, the documentation for each [package][PackageNode] it contains
    /// are then written.
    /// @param moduleNode The module to write documentation for
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputModuleDoc(ModuleNode moduleNode) throws InvalidPathException, IOException {
        ctx.setModuleName(moduleNode.getName().fullyQualifiedName());
        if (!moduleNode.getPackages().isEmpty()) {
            ctx.setModuleName(moduleNode.getName().fullyQualifiedName());
            writer = ctx.createFileInModule("index.md");
            if (moduleNode.getName().isEmpty()) {
                // Unnamed module
                writer.write("# " + api.getTitle() + NL);
            } else {
                writer.write("# " + TITLE_MODULE + " " + moduleNode.getName() + NL);
            }
            writer.write("\n\n" + MarkdownUtils.formatText(moduleNode.getFullBody()) + NL + NL);
            if (moduleNode.getName().isEmpty() && !moduleNode.getPackages().isEmpty()) {
                writer.write("## " + TITLE_PACKAGES + NL + NL);
                MarkdownTable table = new MarkdownTable()
                        .addColumn(TITLE_PACKAGE)
                        .addColumn(TITLE_DESCRIPTION);
                for (PackageNode pkg : moduleNode.getPackages()) {
                    table.addRow(MarkdownUtils.link(pkg.getLink(), true), MarkdownUtils.inOneLine(MarkdownUtils.formatText(pkg.getFirstSentence())));
                }
                table.render(writer);
            }
            outputModuleDirectives(TITLE_EXPORTS, TITLE_PACKAGE, moduleNode.getExports());
            outputModuleDirectives(TITLE_REQUIRES, TITLE_MODULE, moduleNode.getRequires());
            outputModuleDirectives(TITLE_OPENS, TITLE_PACKAGE, moduleNode.getOpens());
            outputModuleDirectives(TITLE_USES, TITLE_INTERFACE, moduleNode.getUses());
            outputModuleProvidesDirectives(moduleNode.getProvides());
            writer.flush();
            writer.close();
            PackageWriter packageWriter = new PackageWriter(api, ctx);
            packageWriter.writeDocs(moduleNode);
        }
        if (!moduleNode.getConstantValues().isEmpty()) {
            outputConstantValues(moduleNode);
        }
        if (!moduleNode.getPackages().isEmpty()){
            outputElementList(moduleNode);
        }
    }

    /// Outputs the directives declared in a module's `module-info.java` file.
    /// @param title The title to be used in this section of the markdown page.
    /// @param kind  The kind of directive being described
    /// @param directives The list of directives belonging to the module.
    /// @param refKind The kind of reference used for first link from this directive
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputModuleDirectives(String title, String kind, List<DirectiveNode> directives) throws IOException {
        if (directives.isEmpty()) return;
        writer.write("=== \"" + title + "\"" + NL + NL);
        MarkdownTable table = new MarkdownTable()
                .addColumn(kind)
                .addColumn(TITLE_DESCRIPTION);
        for (DirectiveNode directive : directives) {
            String columnTwo = formatDirectivePackageDoc(directive);
            Link link = directive.getLink();
            String linkString = MarkdownUtils.link(link, true);
            table.addRow(linkString, columnTwo);
        }
        table.render(writer, 4);
    }

    /// Outputs the `provides` directives declared in a module's `module-info.java` file.
    /// @param directives The list of `provides` directives belonging to the module.
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputModuleProvidesDirectives(List<DirectiveNode> directives) throws IOException {
        if (directives.isEmpty()) return;
        writer.write("\n=== \"" + TITLE_PROVIDES + "\"" + NL + NL);
        MarkdownTable table = new MarkdownTable()
                .addColumn(TITLE_INTERFACE)
                .addColumn(TITLE_IMPLEMENTATIONS);
        for (DirectiveNode directive : directives) {
            if (directive.getName() == null || directive.getName().isEmpty()) {
                ctx.reportError("provides directive with no details");
            }
            Link reference = directive.getLink();
            table.addRow(MarkdownUtils.link(reference, true),
                         multiLink(directive.getImplementations()));
        }
        table.render(writer, 4);
    }

    /// Turns each member of a list of strings into a link to the documentation
    /// for the module, package, or type denoted by that list member.
    /// @param names A list of strings that represent modules, packages, or types.
    private String multiLink(List<Link> references) {
        StringBuilder sb = new StringBuilder();
        for (Link link : references) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            String label = link.getTarget().getName().fullyQualifiedName();
            if (link.isResolved()) {
                sb.append(String.format("[%s](%s)", label, link.getUri()));
            } else {
                sb.append(label);
            }
        }
        return sb.toString();
    }

    /// Formats the first sentence of a [DirectiveNode]'s documentation so
    /// that it uses only one line in the markdown output and links and
    /// code are rendered properly.
    private String formatDirectivePackageDoc(DirectiveNode directive) {
        if (directive.getLink().getTarget().getName() == null) {
            String name = directive.getLink().getTarget().getModuleName();
            ModuleNode modleNode = api.getModuleNode(name);
            if (modleNode != null) {
                return MarkdownUtils.inOneLine(MarkdownUtils.formatText(modleNode.getFirstSentence()));
            }
        } else {
            String name = directive.getLink().getTarget().getName().fullyQualifiedName();
            PackageNode packageNode = api.getPackageNode(name);
            if (packageNode != null) {
                return MarkdownUtils.inOneLine(MarkdownUtils.formatText(packageNode.getFirstSentence()));
            }
            TypeNode typeNode = api.getTypeNode(name);
            if (typeNode != null) {
                return MarkdownUtils.inOneLine(MarkdownUtils.formatText(typeNode.getFirstSentence()));
            }
        }
        return "";
    }

    /// Outputs the *Constant Field Values* page.
    /// @param moduleNode The API tree node representing a module.
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputConstantValues(ModuleNode moduleNode) throws InvalidPathException, IOException {
        writer = ctx.createFileInModule("constant-values.md");
        if (!moduleNode.getConstantValues().isEmpty()) {
            writer.write("# " + TITLE_CONSTANT_FIELD_VALUES + NL);
            MarkdownTable table = new MarkdownTable()
                    .addColumn(TITLE_MODIFIER_AND_TYPE)
                    .addColumn(TITLE_CONSTANT_FIELD)
                    .addColumn(TITLE_VALUE);
            for (FieldNode constantValue : moduleNode.getConstantValues()) {
                StringBuilder modifiersAndType = new StringBuilder();
                if (!constantValue.getModifiersString().isEmpty()) {
                    modifiersAndType.append(constantValue.getModifiersString());
                    modifiersAndType.append(" ");
                }
                VariableTypeNode reference = constantValue.getConstantValueReference();
                modifiersAndType.append(MarkdownUtils.formatVariableTypeNode(reference, true));
                table.addRow(modifiersAndType.toString(), constantValue.getName().simpleName(), escape(constantValue.getConstantValue().toString()));
            }
            table.render(writer);
        }
        writer.flush();
        writer.close();
    }

    private void outputElementList(ModuleNode moduleNode) throws InvalidPathException, IOException {
        writer = ctx.createFileInModule("element-list");
        writer.write("module:");
        writer.write(moduleNode.getName().fullyQualifiedName());
        writer.write(NL);
        for (PackageNode pkg : moduleNode.getPackages()) {
            writer.write(pkg.getName().fullyQualifiedName());
            writer.write(NL);
        }
        writer.flush();
        writer.close();
    }

    private static String escape(String text) {
        return text.replace("[","\\[");
    }
}
