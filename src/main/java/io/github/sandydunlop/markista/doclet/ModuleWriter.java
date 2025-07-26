package io.github.sandydunlop.markista.doclet;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.DirectiveNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageMember;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.util.Configuration;
import io.github.sandydunlop.markista.util.FileUtils;
import io.github.sandydunlop.markista.util.LinkResolver;
import io.github.sandydunlop.markista.util.Markdown;
import io.github.sandydunlop.markista.util.Utils;

public class ModuleWriter {
    private static final String TITLE_API = "API";
    private static final String TITLE_CONSTANT_FIELD = "Constant Field";
    private static final String TITLE_CONSTANT_FIELD_VALUES = "Constant Field Values";
    private static final String TITLE_DESCRIPTION = "Description";
    private static final String TITLE_EXPORTS = "Exports";
    private static final String TITLE_IMPLEMENTATIONS = "Implementations";
    private static final String TITLE_INTERFACES = "Interfaces";
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
    public ModuleWriter() {
        // Nothing to do here
    }

    /// Output the documentation files for the specified API
    /// @param  api The API to output the documentation for
    public void writeDocs(Api api) throws IOException {
        this.api = api;
        for (ModuleNode moduleNode : api.getModules()) {
            outputModuleDoc(moduleNode);
        }
        if (!api.getUnnamedModuleNode().getPackages().isEmpty()) {
            outputModuleDoc(api.getUnnamedModuleNode());
        }
    }

    private void outputModuleDoc(ModuleNode moduleNode) throws IOException {
        LinkResolver.setLocation("");
        fileUtils = new FileUtils(moduleNode, Configuration.getOutputDirectory());
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
        PackageWriter packageWriter = new PackageWriter(moduleDir);
        packageWriter.writeDocs(moduleNode);
        if (!moduleNode.getConstantValues().isEmpty()) {
            outputConstantValues(moduleNode);
        }
    }

    private void outputModuleDirectives(String title, String kind, List<DirectiveNode> directives) throws IOException {
        if (directives.isEmpty()) return;
        writer.write("=== \"" + title + "\"\n\n");
        MarkdownTable table = new MarkdownTable()
                .addColumn(kind)
                .addColumn(TITLE_DESCRIPTION);
        for (DirectiveNode directive : directives) {
            table.addRow(Markdown.mdAutoLink(directive.getName()), getDirectivePackageDoc(directive));
        }
        table.render(writer, 4);
    }

    private void outputModuleProvidesDirectives(List<DirectiveNode> directives) throws IOException {
        if (directives.isEmpty()) return;
        writer.write("=== \"" + TITLE_PROVIDES + "\"\n\n");
        MarkdownTable table = new MarkdownTable()
                .addColumn(TITLE_IMPLEMENTATIONS)
                .addColumn(TITLE_INTERFACES);
        for (DirectiveNode directive : directives) {
            // table.addRow(Markdown.mdAutoLink(directive.getName()),
            //              Markdown.mdAutoLink(directive.getName()));
            table.addRow(multiLink(directive.getImplementations()),
                         multiLink(directive.getInterfaces()));
        }
        table.render(writer, 4);
    }

    private String multiLink(List<String> names) {
        return Markdown.mdAutoLink(names.toString());
    }

    private String getDirectivePackageDoc(DirectiveNode directive) {
        PackageNode pkg = api.getPackageNode(directive.getName());
        if (pkg != null) {
            String formatted = Markdown.formatText(pkg.getFirstSentence());
            return Utils.inOneLine(formatted);
        }
        return "";
    }

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
                if (constantValue.getModifiersString().isEmpty()) {
                    modifiersAndType.append(constantValue.getModifiersString());
                    modifiersAndType.append(" ");
                }
                modifiersAndType.append(Markdown.mdAutoLink(constantValue.getType().getQualifiedName(), true));
                table.addRow(modifiersAndType.toString(), constantValue.getSimpleName(), constantValue.getConstantValue().toString());
            }
            table.render(writer);
        }
        writer.flush();
        writer.close();
    }
}
