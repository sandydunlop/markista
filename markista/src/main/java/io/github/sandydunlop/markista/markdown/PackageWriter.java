package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.PackageOrTypeNode;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.util.Markdown;
import io.github.sandydunlop.markista.util.Configuration;
import io.github.sandydunlop.markista.util.Context;
import io.github.sandydunlop.markista.util.Utils;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/// A class that outputs API package documentation as Markdown.
public class PackageWriter {
    private static final String TEXT_CLASS = "Class";
    private static final String TEXT_DESCRIPTION = "Description";

    /// The Context singleton instance providing access to the current documentation generation context,
    /// including configuration, current module/package/type names, and reporting utilities.
    private final Context ctx = Context.getInstance();

    /// The Writer used to output the generated markdown content for the current document.
    /// It handles writing text to the appropriate output file or stream.
    private Writer writer = null;

    /// Constructor that sets up the locations API documents will be written to.
    public PackageWriter() {
        // Nothing to see here
    }

    /// Output the documentation files for the specified API
    /// @param moduleNode  The module containing the packages to output the documentation for
    /// @throws java.io.IOException if there is a problem writing to the output file
    public void writeDocs(ModuleNode moduleNode) throws IOException {
        for (PackageNode packageNode : moduleNode.getPackages()) {
            outputPackageDoc(packageNode);
        }
    }

    /// Writes the Javadoc for a package as Markdown
    /// @param packageNode the package
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputPackageDoc(PackageNode packageNode) throws IOException {
        ctx.setPackageName(packageNode.getName());
        writer = ctx.createFileInPackage();    
        writer.write("# Package " + packageNode.getName() + "\n");
        writer.write("\n\n" + Markdown.formatText(packageNode.getFullBody()) + "\n\n");
        outputPackageMembers("Packages", packageNode.getPackages());
        outputPackageMembers("Classes", packageNode.getClasses());
        outputPackageMembers("Interfaces", packageNode.getInterfaces());
        outputPackageMembers("Enum Classes", packageNode.getEnums());
        outputPackageMembers("Annotation Types", packageNode.getAnnotations());
        writer.flush();
        writer.close();
        TypeWriter typeWriter = new TypeWriter();
        for (TypeNode member : packageNode.getClasses()) {
            typeWriter.writeDoc(member);
        }
        for (TypeNode member : packageNode.getInterfaces()) {
            typeWriter.writeDoc(member);
        }
        for (TypeNode member : packageNode.getEnums()) {
            typeWriter.writeDoc(member);
        }
        for (TypeNode member : packageNode.getAnnotations()) {
            typeWriter.writeDoc(member);
        }
        ctx.setPackageName("");
    }

    /// Writes the Javadoc for a package's members as Markdown
    /// @param title The title of this section in the Markdown document
    /// @param members The list of members of this package
    /// @throws java.io.IOException if there is a problem writing to the output file
    private void outputPackageMembers(String title, List<? extends PackageOrTypeNode> members) throws IOException {
        if (members.isEmpty()) return;
        String memberKind = TEXT_CLASS;
        if (members.getFirst() instanceof PackageNode) {
            memberKind = "Package";
        }
        MarkdownTable table = new MarkdownTable()
                .addColumn(memberKind)
                .addColumn(TEXT_DESCRIPTION);
        for (PackageOrTypeNode member : members) {
            table.addRow(Markdown.mdDocumentLink(member.getSimpleName()), Utils.inOneLine(Markdown.formatText(member.getFirstSentence())));
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