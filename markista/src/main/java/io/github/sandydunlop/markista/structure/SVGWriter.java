package io.github.sandydunlop.markista.structure;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.Node;
import io.github.sandydunlop.markista.model.PackageMember;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.util.Context;

public class SVGWriter {
    /// The Writer used to output the generated markdown content for the current document.
    /// It handles writing text to the appropriate output file or stream.
    private Writer writer;

    /// The Api model representing the entire documented API structure,
    /// including modules, packages, types, and members used for cross-referencing and navigation.
    Api api;

    ModuleNode module;

    Entry root;
    List<Entry> list;
    int lineHeight = 18;
    int imageHeight = -1;
    int imageWidth = -1;
    int y = 0;
    int indent = 16;

    /// The Context singleton instance providing access to the current documentation generation context,
    /// including configuration, current module/package/type names, and reporting utilities.
    /// > **Warning**<br/>
    /// Do not make this `final`. It will break tests with mocked [Context].
    private Context ctx;

    public SVGWriter() {
        this.ctx = Context.getInstance();
    }

    public void setApi(Api api) {
        this.api = api;
    }
    public void setContext(Context context) {
        ctx = context;
    }

    public void setModule(ModuleNode module) {
        api = null;
        this.module = module;
    }

    public void scan() {
        y = 0;
        list = new ArrayList<>();
        root = addModule(module, null);
        imageHeight = list.size() * lineHeight;
        for (Entry entry : list) {
            if (entry.getLabel().length() > imageWidth) {
                imageWidth = entry.getLabel().length() ;
            }
        }
        imageWidth = (imageWidth + 2) * 12;
        imageHeight +=20;
        System.out.println(String.format("width=%d height=%d", imageWidth, imageHeight));
    }

    //
    //
    //

    private Entry addModule(ModuleNode node, Entry current) {
        Entry entry = new Entry(SVGIcon.module(), node.getName());
        if (current != null) {
            current.addEntry(entry);
            entry.setX(current.getX() + indent);
            entry.setY(y);
            entry.setParent(current);
        }
        y += lineHeight;
        list.add(entry);
        for (PackageMember pkg : node.getPackages()) {
            if (pkg instanceof PackageNode pn) {
                addPackage(pn, entry);
            }
        }
        return entry;
    }

    private void addPackage(PackageNode node, Entry current) {
        Entry entry = new Entry(SVGIcon.pkg(), node.getName());
        if (current != null) {
            current.addEntry(entry);
            entry.setX(current.getX() + indent);
            entry.setY(y);
            entry.setParent(current);
        }
        y += lineHeight;
        list.add(entry);
        for (PackageMember pkg : node.getPackages()) {
            if (pkg instanceof PackageNode pn) {
                addPackage(pn, entry);
            }
        }
        for (TypeNode type : node.getTypes()) {
            addType(type, entry);
        }
    }

    private void addType(TypeNode node, Entry current) {
        Entry entry = new Entry(SVGIcon.file(), node.getSimpleName());
        if (current != null) {
            current.addEntry(entry);
            entry.setX(current.getX() + indent);
            entry.setY(y);
            entry.setParent(current);
        }
        y += lineHeight;
        list.add(entry);
    }

    //
    //
    //

    public void write() throws IOException {
        ctx.setModuleName(module.getName());

        writer = ctx.createFileInModule("structure.svg");
        writer.write(top());

        for (Entry entry : list) {
            writeEntry(entry);
        }
        writer.write(bot());
        writer.flush();
        writer.close();
    }

    private void writeEntry(Entry entry) throws IOException{
        SVGIcon icon = entry.getIcon();
        String name = "module_" + escapeName(entry.getLabel());
        writer.write("  <g data-cell-id=\"" + name + "\">\n");
        writer.write("    <g data-cell-id=\"" + name + "_icon\">\n");
        writer.write(icon.placeAt(entry.getX(), entry.getY()));
        writer.write("    </g>\n");
        writer.write("    <g data-cell-id=\"" + name + "_label\">\n");
        writer.write(String.format("      <text x=\"%d\" y=\"%d\" fill=\"light-dark(#505050, #B9B5B4)\" font-family=\"Helvetica\" font-size=\"12px\">%s</text>\n", entry.getX() + 18, entry.getY() + 10, entry.getLabel()));
        writer.write("    </g>\n");

        if (entry.getParent() != null) {
            writeLines(entry);
        }

        writer.write("  </g>\n");
    }

    private void writeLines(Entry entry) throws IOException {
        writer.write("    <g>\n");
        writer.write(String.format("      <path d=\"M %d %d L %d %d\" fill=\"none\" stroke=\"#505050\" stroke-miterlimit=\"10\" pointer-events=\"stroke\" style=\"stroke: light-dark(#505050, #B9B5B4);\"/>\n",
                entry.getParent().getX() + 7, entry.getY() + 6 ,entry.getX() - 2 , entry.getY() + 6));
        writer.write(String.format("      <path d=\"M %d %d L %d %d\" fill=\"none\" stroke=\"#505050\" stroke-miterlimit=\"10\" pointer-events=\"stroke\" style=\"stroke: light-dark(#505050, #B9B5B4);\"/>\n",
                entry.getParent().getX() + 7, entry.getY() + 6 ,entry.getParent().getX() + 7 , entry.getParent().getY() + 14));
        writer.write("    </g>\n");
    }

    private String escapeName(String name) {
        return name.replace(".", "_");
    }

    private String top() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
                        "<!-- Do not edit this file with editors other than draw.io -->\n" + //
                        "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n" + //
                        "<svg\n" + //
                        "\txmlns=\"http://www.w3.org/2000/svg\" style=\"background: transparent; background-color: transparent; color-scheme: light dark;\"\n" + //
                        String.format("\txmlns:xlink=\"http://www.w3.org/1999/xlink\" version=\"1.1\" width=\"%dpx\" height=\"%dpx\" viewBox=\"0 0  %d %d\" content=\"\">\n", imageWidth, imageHeight, imageWidth, imageHeight) + //
                        "\t<defs/>\n" + //
                        "\t<g>\n" + //
                        "\t\t<g data-cell-id=\"0\">\n" + //
                        "\t\t\t<g data-cell-id=\"1\">";        
    }

    private String bot() {
        return "\t\t\t</g>\n" + //
                        "\t\t</g>\n" + //
                        "\t</g>\n" + //
                        "</svg>\n";
    }
}
