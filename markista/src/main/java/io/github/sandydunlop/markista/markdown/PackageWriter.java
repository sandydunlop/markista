package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.core.Configuration;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.Link;
import io.github.sandydunlop.markista.model.TypeNode;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.InvalidPathException;
import java.util.List;

/// A class that outputs API package documentation as Markdown.
public class PackageWriter {
    private static final String TEXT_CLASS = "Class";
    private static final String TEXT_DESCRIPTION = "Description";

    /// The Context singleton instance providing access to the current documentation generation context,
    /// including configuration, current module/package/type names, and reporting utilities.
    /// > **Warning**<br/>
    /// Do not make this `final`. It will break tests with mocked [Context].
    private Context ctx;

    /// The Writer used to output the generated markdown content for the current document.
    /// It handles writing text to the appropriate output file or stream.
    private Writer writer = null;

    /// Constructor that sets up the locations API documents will be written to.
    public PackageWriter(Context context) {
        ctx = context;
    }

    /// Output the documentation files for the specified API
    /// @param moduleNode  The module containing the packages to output the documentation for
    /// @throws java.io.IOException if there is a problem writing to the output file
    public void writeDocs(ModuleNode moduleNode) throws InvalidPathException, IOException {
        for (PackageNode packageNode : moduleNode.getPackages()) {
            outputPackageDoc(packageNode);
        }
    }

    /// Writes the Javadoc for a package as Markdown
    /// @param packageNode the package
    /// @throws java.io.IOException if there is a problem writing to the output file
    void outputPackageDoc(PackageNode packageNode) throws InvalidPathException, IOException {
        ctx.setPackageName(packageNode.getName());
        writer = ctx.createFileInPackage();
        writer.write("\n");
        writer.write("# Package " + packageNode.getName() + "\n");
        writer.write("\n\n" + MarkdownUtils.formatText(packageNode.getFullBody()) + "\n\n");
        outputPackageMemberPackages("Packages", packageNode.getPackages());
        outputPackageMemberTypes("All Classes and Interfaces", packageNode.getTypes());
        outputPackageMemberTypes("Interfaces", packageNode.getInterfaces());
        outputPackageMemberTypes("Classes", packageNode.getClasses());
        outputPackageMemberTypes("Record Classes", packageNode.getRecords());
        outputPackageMemberTypes("Enum Classes", packageNode.getEnums());
        outputPackageMemberTypes("Annotation Interfaces", packageNode.getAnnotations());
        writer.flush();
        writer.close();
        TypeWriter typeWriter = new TypeWriter(ctx);
        for (TypeNode member : packageNode.getClasses()) {
            typeWriter.outputTypeDoc((TypeNode)member);
        }
        for (TypeNode member : packageNode.getInterfaces()) {
            typeWriter.outputTypeDoc((TypeNode)member);
        }
        for (TypeNode member : packageNode.getRecords()) {
            typeWriter.outputTypeDoc((TypeNode)member);
        }
        for (TypeNode member : packageNode.getEnums()) {
            typeWriter.outputTypeDoc((TypeNode)member);
        }
        for (TypeNode member : packageNode.getAnnotations()) {
            typeWriter.outputTypeDoc((TypeNode)member);
        }
        ctx.setPackageName("");
    }

    /// Writes the Javadoc for a package's member packages as Markdown
    /// @param title The title of this section in the Markdown document
    /// @param members The list of members of this package
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputPackageMemberPackages(String title, List<PackageNode> members) throws IOException {
        if (members.isEmpty()) return;
        MarkdownTable table = new MarkdownTable()
                .addColumn(
                        "Package")
                .addColumn(TEXT_DESCRIPTION);
        for (PackageNode member : members) {
            String name = member.getName();
            name = name.substring(name.lastIndexOf(".") + 1);
            Link link = Link.to(member.getName())
                    .from(ctx.getPackageName())
                    .withKind(Link.Kind.PACKAGE)
                    .withLabel(name)
                    .withUri(name);
            table.addRow(MarkdownUtils.link(link, false), MarkdownUtils.inOneLine(MarkdownUtils.formatText(member.getFirstSentence())));
        }
        if (Configuration.getUseContentTabs()) {
            writer.write("=== \"" + title + "\"\n\n");
            table.render(writer, 4);
        } else {
            writer.write("\n\n## " + title + "\n\n");
            table.render(writer);
        }
    }

    /// Writes the Javadoc for a package's members as Markdown
    /// @param title The title of this section in the Markdown document
    /// @param members The list of members of this package
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputPackageMemberTypes(String title, List<TypeNode> members) throws IOException {
        if (members.isEmpty())
            return;
        MarkdownTable table = new MarkdownTable()
                .addColumn(
                        TEXT_CLASS)
                .addColumn(TEXT_DESCRIPTION);
        for (TypeNode member : members) {
            table.addRow(MarkdownUtils.mdDocumentLink(member.getSimpleName()),
                    MarkdownUtils.inOneLine(MarkdownUtils.formatText(member.getFirstSentence())));
        }
        if (Configuration.getUseContentTabs()) {
            writer.write("=== \"" + title + "\"\n\n");
            table.render(writer, 4);
        } else {
            writer.write("\n\n## " + title + "\n\n");
            table.render(writer);
        }
    }
}