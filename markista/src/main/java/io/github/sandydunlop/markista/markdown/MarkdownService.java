package io.github.sandydunlop.markista.markdown;

import java.io.IOException;

import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.spi.DocService;

public class MarkdownService implements DocService {
    @Override
    public boolean replacesDefault() {
        return false;
    }

    public boolean start(Api api, Context ctx) {
        // The Writer used to output the generated markdown content for the current document.
        // It handles writing text to the appropriate output file or stream.
        try {
            ModuleWriter writer = new ModuleWriter();
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
