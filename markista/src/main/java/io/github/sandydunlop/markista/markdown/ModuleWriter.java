package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.DirectiveNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageMember;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.util.Configuration;
import io.github.sandydunlop.markista.util.Context;
import io.github.sandydunlop.markista.util.FileUtils;
import io.github.sandydunlop.markista.util.Markdown;
import io.github.sandydunlop.markista.util.Utils;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/// A class that outputs a module's API documentation as Markdown.
public class ModuleWriter {
    private static final String TITLE_API = "API";
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

    private FileUtils fileUtils;
    private Writer writer;
    private Api api;

    /// Constructor that sets up the locations API documents will be written to.
    public ModuleWriter(FileUtils fileUtils) {
        this.fileUtils = fileUtils;
        // Nothing to do here
    }

    /// Output the documentation files for the specified API
    /// @param  api The API to output the documentation for
    /// @throws java.io.IOException
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
    /// @throws java.io.IOException
    private void outputModuleDoc(ModuleNode moduleNode) throws IOException {
        Context.setModuleName(moduleNode.getName());
        fileUtils.setModule(moduleNode);

        if (!moduleNode.getPackages().isEmpty()) {
            writer = fileUtils.createModuleFile(moduleNode.getName(), "index.md");
            if (moduleNode.getName().isEmpty()) {
                writer.write("# " + TITLE_API + "\n");
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
                    table.addRow(Markdown.mdAutoLink(member.getName()), Utils.inOneLine(Markdown.formatText(member.getDescription())));
                }
                table.render(writer);
            }
            outputModuleDirectives(TITLE_EXPORTS, TITLE_PACKAGE, moduleNode.getExports());
            outputModuleDirectives(TITLE_REQUIRES, TITLE_MODULE, moduleNode.getRequires());
            outputModuleDirectives(TITLE_OPENS, TITLE_PACKAGE, moduleNode.getOpens());
            outputModuleDirectives(TITLE_USES, TITLE_PACKAGE, moduleNode.getUses());
            outputModuleProvidesDirectives(moduleNode.getProvides());
            writer.flush();
            writer.close();
            String moduleDir = Configuration.getOutputDirectory() + "/" + moduleNode.getName();
            PackageWriter packageWriter = new PackageWriter(new FileUtils(moduleDir));
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
    /// @throws java.io.IOException
    private void outputModuleDirectives(String title, String kind, List<DirectiveNode> directives) throws IOException {
        if (directives.isEmpty()) return;
        writer.write("=== \"" + title + "\"\n\n");
        MarkdownTable table = new MarkdownTable()
                .addColumn(kind)
                .addColumn(TITLE_DESCRIPTION);
        for (DirectiveNode directive : directives) {
            table.addRow(Markdown.mdAutoLink(directive.getName()), formatDirectivePackageDoc(directive));
        }
        table.render(writer, 4);
    }

    /// Outputs the `provides` directives declared in a module's `module-info.java` file.
    /// @param directives The list of `provides` directives belonging to the module.
    /// @throws java.io.IOException
    private void outputModuleProvidesDirectives(List<DirectiveNode> directives) throws IOException {
        if (directives.isEmpty()) return;
        writer.write("\n=== \"" + TITLE_PROVIDES + "\"\n\n");
        MarkdownTable table = new MarkdownTable()
                .addColumn(TITLE_INTERFACE)
                .addColumn(TITLE_IMPLEMENTATIONS);
        for (DirectiveNode directive : directives) {
            table.addRow(Markdown.mdAutoLink(directive.getInterface(), false),
                         multiLink(directive.getImplementations()));
        }
        table.render(writer, 4);
    }

    /// Turns each member of a list of strings into a link to the documentation
    /// for the module, package, or type denoted by that list member.
    /// @param names A list of strings that represent modules, packages, or types.
    private String multiLink(List<String> names) {
        return Markdown.mdAutoLink(String.join(",",names), false);
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
    /// @throws java.io.IOException
    private void outputConstantValues(ModuleNode moduleNode) throws IOException {
        writer = fileUtils.createModuleFile(moduleNode.getName(), "constant-values.md");    
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
                modifiersAndType.append(Markdown.mdAutoLink(constantValue.getType().getQualifiedName(), true));
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
}
