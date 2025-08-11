package io.github.sandydunlop.markista.structure;

import java.io.IOException;
import java.nio.file.Path;

import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.util.Configuration;
import io.github.sandydunlop.markista.util.Context;

public class Structures {
    ModuleNode module;

    /// The Context singleton instance providing access to the current documentation generation context,
    /// including configuration, current module/package/type names, and reporting utilities.
    /// > **Warning**<br/>
    /// Do not make this `final`. It will break tests with mocked [Context].
    private Context ctx;

    public Structures() {
        this.ctx = Context.getInstance();
    }

    public void setContext(Context context) {
        ctx = context;
    }

    public void setModule(ModuleNode module) {
        this.module = module;
    }

    public void run() throws IOException {
        TreeSvgWriter writer = new TreeSvgWriter();
        writer.setContext(ctx);

        FileTree docTree = new FileTree();
        docTree.setModule(module);
        docTree.scan(Path.of(ctx.getOutputDirectory(), module.getName()));
        writer.write(docTree, "docs.svg");

        if (Configuration.getProjectPath() != null) {
            FileTree sourceTree = new FileTree();
            sourceTree.setModule(module);
            sourceTree.ignore("build");
            sourceTree.ignore("bin");
            sourceTree.scan(Path.of(Configuration.getProjectPath()));
            writer.write(sourceTree, "source.svg");
        }

        ModuleTree moduleTree = new ModuleTree();
        moduleTree.setModule(module);
        moduleTree.scan();
        writer.write(moduleTree, "module.svg");
    }
}
