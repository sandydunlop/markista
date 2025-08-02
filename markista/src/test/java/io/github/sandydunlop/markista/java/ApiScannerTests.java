package io.github.sandydunlop.markista.java;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import com.sun.source.util.DocTreePath;

import io.github.sandydunlop.markista.markdown.PackageWriter;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.util.Configuration;
import io.github.sandydunlop.markista.util.Context;
import io.github.sandydunlop.markista.util.FileUtils;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class ApiScannerTests {
    Api api;
    FileUtils files;
    ModuleNode moduleNode;
    PackageNode packageNode;
    PackageWriter packageWriter;

    @Mock static Reporter reporter = new Reporter() {
        @Override
        public void print(Kind kind, String message) {
            System.out.println(kind + ": " + message);
        }

        @Override
        public void print(Kind kind, DocTreePath path, String message) {
            // Do nothing
        }

        @Override
        public void print(Kind kind, Element element, String message) {
            // Do nothing
        }
    };
    
    @BeforeAll
    static void initAll() {
		Context.setReporter(reporter);
        Configuration.setOutputDirectory("/tmp/doc");
    }

    @BeforeEach
    void init() {
        files = new FileUtils("/tmp/doc");
        files.setModule(moduleNode);
    }

    @Test
    void scan_init() {
        List<? extends Element> elements = List.of();
        assertNotNull(elements);
    }
}
