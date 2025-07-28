package io.github.sandydunlop.markista.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.source.util.DocTreePath;

import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;

import jdk.javadoc.doclet.Reporter;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class FilesTests {
    FileUtils files;
    ModuleNode module;

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
		Configuration.setReporter(reporter);
    }

    @BeforeEach
    void init() {
        module = new ModuleNode("sandydunlop.markista");
        files = new FileUtils(module, "/tmp/doc");
    }

    @Test
    void countDots_1() {
        assertEquals(1, files.countDots("one.x"));
    }

    @Test
    void countDots_2() {
        assertEquals(2, files.countDots("one.two.x"));
    }

    @Test
    void countDots_3() {
        assertEquals(3, files.countDots("one.two.three.x"));
    }

    @Test
    void flattenDirectories() {
        Configuration.setFlattenDirectories(true);
		module.addPackage(new PackageNode("io.github.sandydunlop"));
		PackageNode markista = new PackageNode("io.github.sandydunlop.markista");
		PackageNode util = new PackageNode("io.github.sandydunlop.markista.util");
		PackageNode doclet = new PackageNode("io.github.sandydunlop.markista.doclet");
        module.addPackage(markista);
        module.addPackage(util);
        module.addPackage(doclet);
        files.setFlattenedDirectories(module);
        assertEquals("io.github", files.getFlattenedDirectories());
    }
}
