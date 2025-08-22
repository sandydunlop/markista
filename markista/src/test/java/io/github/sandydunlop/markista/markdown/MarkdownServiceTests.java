package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;

import java.io.StringWriter;
import java.nio.file.InvalidPathException;

import javax.lang.model.element.Element;

import jdk.javadoc.doclet.Reporter;

import com.sun.source.util.DocTreePath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownServiceTests {
    @AfterEach
    void resetSingleton() {
        Context.reset();
    }

    @Test
    void doesntReplaceItself() {
        MarkdownService service = new MarkdownService();
        assertFalse(service.replacesDefault());
    }

    @Test
    void exceptionHandling() {
        Context ctx = Context.getInstance();

        // Invalid destination
        ctx.setOutputDirectory("\u0000");

        // Create an API with only one empty module
        Api api = new Api("Test API");
        ModuleNode module = new ModuleNode("module");
        PackageNode pkg = new PackageNode("package");
        module.addPackage(pkg);
        api.addModule(module);

        // Ensure an exception is thrown
        MarkdownService service = new MarkdownService();
        assertThrows(InvalidPathException.class, () -> {
            service.start(api, ctx);
        });
    }    

    @Test
    void finishSucceeds() {
        MarkdownService service = new MarkdownService();
        assertTrue(service.finish());
    }

    class TestReporter implements Reporter {
        public StringWriter stringWriter;

        @Override
        public void print(javax.tools.Diagnostic.Kind kind, String message) {
            stringWriter.write(message);
        }

        @Override
        public void print(javax.tools.Diagnostic.Kind kind, DocTreePath path, String message) {
            stringWriter.write(message);
        }

        @Override
        public void print(javax.tools.Diagnostic.Kind kind, Element element, String message) {
            stringWriter.write(message);
        }
    }       
}
