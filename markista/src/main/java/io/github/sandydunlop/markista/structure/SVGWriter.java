package io.github.sandydunlop.markista.structure;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ModuleNode;
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

    TreeNode root;
    List<TreeNode> list;
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
        for (TreeNode treeNode : list) {
            if (treeNode.getLabel().length() > imageWidth) {
                imageWidth = treeNode.getLabel().length() ;
            }
        }
        imageWidth = (imageWidth + 2) * 12;
        imageHeight +=20;
        System.out.println(String.format("width=%d height=%d", imageWidth, imageHeight));
    }

    //
    //
    //

    private TreeNode addModule(ModuleNode node, TreeNode current) {
        TreeNode treeNode = new TreeNode(SVGIcon.module(), node.getName());
        if (current != null) {
            current.addChild(treeNode);
            treeNode.setX(current.getX() + indent);
            treeNode.setY(y);
            treeNode.setParent(current);
        }
        y += lineHeight;
        list.add(treeNode);
        for (PackageMember pkg : node.getPackages()) {
            if (pkg instanceof PackageNode pn) {
                addPackage(pn, treeNode);
            }
        }
        return treeNode;
    }

    private void addPackage(PackageNode node, TreeNode current) {
        TreeNode treeNode = new TreeNode(SVGIcon.pkg(), node.getName());
        if (current != null) {
            current.addChild(treeNode);
            treeNode.setX(current.getX() + indent);
            treeNode.setY(y);
            treeNode.setParent(current);
        }
        y += lineHeight;
        list.add(treeNode);
        for (PackageMember pkg : node.getPackages()) {
            if (pkg instanceof PackageNode pn) {
                addPackage(pn, treeNode);
            }
        }
        for (TypeNode type : node.getTypes()) {
            addType(type, treeNode);
        }
    }

    private void addType(TypeNode node, TreeNode current) {
        TreeNode treeNode = new TreeNode(SVGIcon.file(), node.getSimpleName());
        if (current != null) {
            current.addChild(treeNode);
            treeNode.setX(current.getX() + indent);
            treeNode.setY(y);
            treeNode.setParent(current);
        }
        y += lineHeight;
        list.add(treeNode);
    }

    //
    //
    //

    public void write() throws IOException {
        ctx.setModuleName(module.getName());

        writer = ctx.createFileInModule("structure.svg");
        writer.write(top());

        for (TreeNode treeNode : list) {
            writeEntry(treeNode);
        }
        writer.write(bot());
        writer.flush();
        writer.close();
    }

    private void writeEntry(TreeNode treeNode) throws IOException{
        SVGIcon icon = treeNode.getIcon();
        String name = "module_" + escapeName(treeNode.getLabel());
        writer.write("  <g data-cell-id=\"" + name + "\">\n");
        writer.write("    <g data-cell-id=\"" + name + "_icon\">\n");
        writer.write(icon.placeAt(treeNode.getX(), treeNode.getY()));
        writer.write("    </g>\n");
        writer.write("    <g data-cell-id=\"" + name + "_label\">\n");
        writer.write(String.format("      <text x=\"%d\" y=\"%d\" fill=\"light-dark(#505050, #B9B5B4)\" font-family=\"Helvetica\" font-size=\"12px\">%s</text>\n", treeNode.getX() + 18, treeNode.getY() + 10, treeNode.getLabel()));
        writer.write("    </g>\n");

        if (treeNode.getParent() != null) {
            writeLines(treeNode);
        }

        writer.write("  </g>\n");
    }

    private void writeLines(TreeNode treeNode) throws IOException {
        writer.write("    <g>\n");
        writer.write(String.format("      <path d=\"M %d %d L %d %d\" fill=\"none\" stroke=\"#505050\" stroke-miterlimit=\"10\" pointer-events=\"stroke\" style=\"stroke: light-dark(#505050, #B9B5B4);\"/>\n",
                treeNode.getParent().getX() + 7, treeNode.getY() + 6 ,treeNode.getX() - 2 , treeNode.getY() + 6));
        writer.write(String.format("      <path d=\"M %d %d L %d %d\" fill=\"none\" stroke=\"#505050\" stroke-miterlimit=\"10\" pointer-events=\"stroke\" style=\"stroke: light-dark(#505050, #B9B5B4);\"/>\n",
                treeNode.getParent().getX() + 7, treeNode.getY() + 6 ,treeNode.getParent().getX() + 7 , treeNode.getParent().getY() + 14));
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
