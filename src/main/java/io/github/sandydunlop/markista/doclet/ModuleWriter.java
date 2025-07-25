package io.github.sandydunlop.markista.doclet;

import java.io.IOException;
import java.io.Writer;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageMember;
import io.github.sandydunlop.markista.util.Files;
import io.github.sandydunlop.markista.util.Markdown;
import io.github.sandydunlop.markista.util.Util;

public class ModuleWriter {
    private static final String TEXT_PACKAGE = "Package";
    private static final String TEXT_DESCRIPTION = "Description";
    private static final String TEXT_MODIFIER_AND_TYPE = "Modifier and Type";

    private Files fileUtils;

    /// Constructor that sets up the locations API documents will be written to.
    public ModuleWriter() {
        // Nothing to do here
    }

    /// Output the documentation files for the specified API
    /// @param  api The API to output the documentation for
    public void writeDocs(Api api) throws IOException {
        for (ModuleNode moduleNode : api.getModules()) {
            outputModuleDoc(moduleNode);
        }
        if (!api.getUnnamedModuleNode().getPackages().isEmpty()) {
            outputModuleDoc(api.getUnnamedModuleNode());
        }
    }

    private void outputModuleDoc(ModuleNode moduleNode) throws IOException {
        fileUtils = new Files(moduleNode, Configuration.getOutputDirectory());
        Writer writer = fileUtils.createModuleFile(moduleNode.getName(), "index.md");
        if (moduleNode.getName().isEmpty()) {
            writer.write("# API\n");
        } else {
            writer.write("# Module " + moduleNode.getName() + "\n");
        }
        writer.write("\n\n" + Markdown.formatText(moduleNode.getFullBody()) + "\n\n");
        if (!moduleNode.getPackages().isEmpty()) {
            writer.write("## Packages\n\n");
            MarkdownTable table = new MarkdownTable()
                    .addColumn(TEXT_PACKAGE)
                    .addColumn(TEXT_DESCRIPTION);
            for (PackageMember member : moduleNode.getPackages()) {
                table.addRow(formatPackageLink(member), Util.inOneLine(Markdown.formatText(member.getDescription())));
            }
            table.render(writer);
        }
        writer.flush();
        writer.close();
        String moduleDir = Configuration.getOutputDirectory() + "/" + moduleNode.getName();
        PackageWriter packageWriter = new PackageWriter(moduleDir);
        packageWriter.writeDocs(moduleNode);
        if (!moduleNode.getConstantValues().isEmpty()) {
            outputConstantValues(moduleNode);
        }
    }

    private void outputConstantValues(ModuleNode moduleNode) throws IOException {
        Writer writer = fileUtils.createModuleFile(moduleNode.getName(), "constant-values.md");    
        if (!moduleNode.getConstantValues().isEmpty()) {
            writer.write("# Constant Field Values\n");
            MarkdownTable table = new MarkdownTable()
                    .addColumn(TEXT_MODIFIER_AND_TYPE)
                    .addColumn("Constant Field")
                    .addColumn("Value");
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

    private String formatPackageLink(PackageMember member) {
        String name = member.getName();
        if (Configuration.getFlattenDirectories() && fileUtils.getFlattenedDirectories() != null) {
            String flattened = fileUtils.getFlattenedDirectories();
            if (member.getName().startsWith(flattened)) {
                name = member.getName().substring(flattened.length() + 1).replace('.', fileUtils.pathSeparator());
            }
        }
        String path = name.replace('.', fileUtils.pathSeparator()) + fileUtils.pathSeparator() + "index.md";
        return Markdown.mdDocumentLink(member.getName(), path);
    }
}
