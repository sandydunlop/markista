package io.github.sandydunlop.markista.structure;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageMember;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.TypeNode;
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
        ModuleTree moduleTree = new ModuleTree();
        moduleTree.setModule(module);
        moduleTree.setContext(ctx);
        moduleTree.scan();
        moduleTree.write("module.svg");

        DirectoryTree directoryTree = new DirectoryTree();
        directoryTree.setModule(module);
        directoryTree.build(moduleTree.getRoot(), moduleTree.getContents());
        directoryTree.write("files.svg");
    }


}
