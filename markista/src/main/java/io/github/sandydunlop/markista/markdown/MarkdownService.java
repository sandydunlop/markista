package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.core.Context;
import io.github.qishr.cascara.lang.java.model.SemanticModel;
import io.github.sandydunlop.markista.spi.DocService;

import java.io.IOException;

public class MarkdownService implements DocService {
    @Override
    public boolean replacesDefault() {
        return false;
    }

    public boolean start(SemanticModel api, Context ctx) {
        // The Writer used to output the generated markdown content for the current document.
        // It handles writing text to the appropriate output file or stream.
        try {
            ModuleWriter writer = new ModuleWriter(ctx);
            writer.writeDocs(api);
        } catch (IOException ex) {
            ctx.reportError(ex.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean finish() {
        return true;
    }
}
