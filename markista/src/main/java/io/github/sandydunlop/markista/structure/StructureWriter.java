package io.github.sandydunlop.markista.structure;

import java.io.IOException;

import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.util.Context;

public class StructureWriter {
    ModuleNode module;

    /// The Context singleton instance providing access to the current documentation generation context,
    /// including configuration, current module/package/type names, and reporting utilities.
    /// > **Warning**<br/>
    /// Do not make this `final`. It will break tests with mocked [Context].
    private Context ctx;

    public StructureWriter() {
        this.ctx = Context.getInstance();
    }

    public void setContext(Context context) {
        ctx = context;
    }

    public void setModule(ModuleNode module) {
        this.module = module;
    }

    public void run() throws IOException {
        FlattenedTree flattenedTree = new FlattenedTree();
        flattenedTree.setModule(module);
        flattenedTree.scan();
        SvgTreeWriter writer = new SvgTreeWriter();
        writer.setContext(ctx);
        writer.write(flattenedTree, TreeKind.MODULE, "module.svg");

        RegularTree regularTree = new RegularTree();
        regularTree.setModule(module);
        regularTree.build(flattenedTree.getRoot(), flattenedTree.getContents());
        writer.setContext(ctx);
        writer.write(regularTree, TreeKind.FILES, "files.svg");

        writer.setContext(ctx);
        writer.write(regularTree, TreeKind.DOCS, "doc-regular.svg");

        writer.setContext(ctx);
        writer.write(flattenedTree, TreeKind.DOCS, "doc-flattened.svg");
    }
}
