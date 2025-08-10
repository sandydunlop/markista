package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.DirectiveNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageMember;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.structure.SVGWriter;
import io.github.sandydunlop.markista.util.Context;
import io.github.sandydunlop.markista.util.Markdown;
import io.github.sandydunlop.markista.util.Utils;

import java.io.IOException;
import java.io.Writer;
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

    /// The Context singleton instance providing access to the current documentation generation context,
    /// including configuration, current module/package/type names, and reporting utilities.
    /// > **Warning**<br/>
    /// Do not make this `final`. It will break tests with mocked [Context].
    private Context ctx;

    /// The Writer used to output the generated markdown content for the current document.
    /// It handles writing text to the appropriate output file or stream.
    private Writer writer;

    /// The Api model representing the entire documented API structure,
    /// including modules, packages, types, and members used for cross-referencing and navigation.
    private Api api;

    /// Constructor that sets up the locations API documents will be written to.
    public ModuleWriter() {
        this.ctx = Context.getInstance();
    }

    /// Output the documentation files for the specified API
    /// @param  api The API to output the documentation for
    /// @throws java.io.IOException if there is a problem writing to the output file
    public void writeDocs(Api api) throws IOException {
        this.api = api;
        for (ModuleNode moduleNode : api.getModules()) {
            outputModuleDoc(moduleNode);
        }
        outputModuleDoc(api.getUnnamedModuleNode());
    }

    /// Outputs a single module's documentation as a Markdown file.
    /// For each module, the documentation for each [package][PackageNode] it contains
    /// are then written.
    /// @param moduleNode The module to write documentation for
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputModuleDoc(ModuleNode moduleNode) throws IOException {
        ctx.setModuleName(moduleNode.getName());
        if (!moduleNode.getPackages().isEmpty()) {
            outputStructureSvg(moduleNode);
            ctx.setModuleName(moduleNode.getName());
            writer = ctx.createFileInModule("index.md");
            if (moduleNode.getName().isEmpty()) {
                writer.write("# " + api.getName() + "\n");
            } else {
                writer.write("# " + TITLE_MODULE + " " + moduleNode.getName() + "\n");
            }
            writer.write("\n\n" + Markdown.formatText(moduleNode.getFullBody()) + "\n\n");
            if (moduleNode.getName().isEmpty() && !moduleNode.getPackages().isEmpty()) {
                writer.write("## " + TITLE_PACKAGES + "\n\n");
                MarkdownTable table = new MarkdownTable()
                        .addColumn(TITLE_PACKAGE)
                        .addColumn(TITLE_DESCRIPTION);
                for (PackageMember member : moduleNode.getPackages()) {
                    table.addRow(Markdown.link(Reference.to(member.getName())), Utils.inOneLine(Markdown.formatText(member.getDescription())));
                }
                table.render(writer);
            }
            outputModuleDirectives(TITLE_EXPORTS, TITLE_PACKAGE, moduleNode.getExports(), Reference.Kind.PACKAGE);
            outputModuleDirectives(TITLE_REQUIRES, TITLE_MODULE, moduleNode.getRequires(), Reference.Kind.MODULE);
            outputModuleDirectives(TITLE_OPENS, TITLE_PACKAGE, moduleNode.getOpens(), Reference.Kind.PACKAGE);
            outputModuleDirectives(TITLE_USES, TITLE_INTERFACE, moduleNode.getUses(), Reference.Kind.TYPE);
            outputModuleProvidesDirectives(moduleNode.getProvides());
            writer.flush();
            writer.close();
            PackageWriter packageWriter = new PackageWriter();
            packageWriter.writeDocs(moduleNode);
        }
        if (!moduleNode.getConstantValues().isEmpty()) {
            outputConstantValues(moduleNode);
        }
    }

    /// Outputs the directives declared in a module's `module-info.java` file.
    /// @param title The title to be used in this section of the markdown page.
    /// @param kind  The kind of directive being described
    /// @param directives The list of directives belonging to the module.
    /// @param refKind The kind of reference used for first link from this directive
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputModuleDirectives(String title, String kind, List<DirectiveNode> directives, Reference.Kind refKind) throws IOException {
        if (directives.isEmpty()) return;
        writer.write("=== \"" + title + "\"\n\n");
        MarkdownTable table = new MarkdownTable()
                .addColumn(kind)
                .addColumn(TITLE_DESCRIPTION);
        for (DirectiveNode directive : directives) {
            Reference moduleRef = Reference.to(directive.getName()).withKind(refKind);
            String columnTwo = formatDirectivePackageDoc(directive);
            String link = Markdown.link(moduleRef);
            table.addRow(link, columnTwo);
        }
        table.render(writer, 4);
    }

    /// Outputs the `provides` directives declared in a module's `module-info.java` file.
    /// @param directives The list of `provides` directives belonging to the module.
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputModuleProvidesDirectives(List<DirectiveNode> directives) throws IOException {
        if (directives.isEmpty()) return;
        writer.write("\n=== \"" + TITLE_PROVIDES + "\"\n\n");
        MarkdownTable table = new MarkdownTable()
                .addColumn(TITLE_INTERFACE)
                .addColumn(TITLE_IMPLEMENTATIONS);
        for (DirectiveNode directive : directives) {
            if (directive.getName() == null || directive.getName().isEmpty()) {
                ctx.reportError("provides directive with no details");
            }
            Reference moduleRef = Reference.to(directive.getName()).withKind(Reference.Kind.TYPE);
            table.addRow(Markdown.link(moduleRef, true),
                         multiLink(directive.getImplementations()));
        }
        table.render(writer, 4);
    }

    /// Turns each member of a list of strings into a link to the documentation
    /// for the module, package, or type denoted by that list member.
    /// @param names A list of strings that represent modules, packages, or types.
    private String multiLink(List<String> names) {
        return Markdown.link(Reference.to(String.join(",",names)), true);
    }

    /// Formats the first sentence of a [DirectiveNode]'s documentation so
    /// that it uses only one line in the markdown output and links and
    /// code are rendered properly.
    private String formatDirectivePackageDoc(DirectiveNode directive) {
        PackageNode pkg = api.getPackageNode(directive.getName());
        if (pkg != null) {
            return Utils.inOneLine(Markdown.formatText(pkg.getFirstSentence()));
        }
        return "";
    }

    /// Output the *Constant Field Values* page.
    /// @param moduleNode The API tree node representing a module.
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputConstantValues(ModuleNode moduleNode) throws IOException {
        writer = ctx.createFileInModule("constant-values.md");    
        if (!moduleNode.getConstantValues().isEmpty()) {
            writer.write("# " + TITLE_CONSTANT_FIELD_VALUES + "\n");
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
                modifiersAndType.append(Markdown.link(Reference.to(constantValue.getType().getQualifiedName()), true));
                table.addRow(modifiersAndType.toString(), constantValue.getSimpleName(), escape(constantValue.getConstantValue().toString()));
            }
            table.render(writer);
        }
        writer.flush();
        writer.close();
    }

    private static String escape(String text) {
        return text.replace("[","\\[");
    }

    private void outputStructureSvg(ModuleNode moduleNode) throws IOException {
        SVGWriter svgWriter = new SVGWriter();
        svgWriter.setModule(moduleNode);
        svgWriter.scan();
        svgWriter.write();
    }
}
