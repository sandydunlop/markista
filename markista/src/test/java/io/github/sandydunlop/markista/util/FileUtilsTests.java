package io.github.sandydunlop.markista.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

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

class FileUtilsTests {
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
    
    boolean fileExists(String file) {
        return Files.exists(Paths.get(file));
    }

    boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    @BeforeAll
    static void initAll() {
		Configuration.setReporter(reporter);
    }

    @BeforeEach
    void init() {
        module = new ModuleNode("markista");
        files = new FileUtils(module, "/tmp/doc");
    }

    @Test
   void buildContainingDirPath() {
        String outputDirectory = "/tmp/doc/run/";
        String packageName = "org.mockito";
        File f = files.buildContainingDirPath(outputDirectory, packageName);
        assertEquals("/tmp/doc/run/org/mockito", f.getAbsolutePath());
    }

    @Test
    void countDots_1() {
        // This has actually failed once when another test running 
        // in parallel has a stack overflow
        assertEquals(1, FileUtils.countDots("one.x"));
    }

    @Test
    void countDots_2() {
        assertEquals(2, FileUtils.countDots("one.two.x"));
    }

    @Test
    void countDots_3() {
        assertEquals(3, FileUtils.countDots("one.two.three.x"));
    }

    @Test
    void flattenDirectories_1() {
        Configuration.setFlattenDirectories(true);
		module.addPackage(new PackageNode("io.github.sandydunlop"));
		PackageNode markista = new PackageNode("io.github.sandydunlop.markista");
		PackageNode util = new PackageNode("io.github.sandydunlop.markista.util");
		PackageNode doclet = new PackageNode("io.github.sandydunlop.markista.doclet");
        module.addPackage(markista);
        module.addPackage(util);
        module.addPackage(doclet);
        files.setFlattenedDirectories(module);
        assertEquals("io.github.sandydunlop", files.getFlattenedDirectories());
    }

    @Test
    void flattenDirectories_2() {
        Configuration.setFlattenDirectories(true);
		PackageNode sandydunlop = new PackageNode("io.github.sandydunlop");
		PackageNode github = new PackageNode("io.github");
		PackageNode markista = new PackageNode("io.github.sandydunlop.markista");
		PackageNode util = new PackageNode("io.github.sandydunlop.markista.util");
		PackageNode doclet = new PackageNode("io.github.sandydunlop.markista.doclet");
        module.addPackage(sandydunlop);
        module.addPackage(github);
        module.addPackage(markista);
        module.addPackage(util);
        module.addPackage(doclet);
        files.setFlattenedDirectories(module);
        assertEquals("io.github", files.getFlattenedDirectories());
    }

    @Test
    void commonBase() {
        List<String> packageNames = List.of(
                "io.github.sandydunlop.markista.doclet",
                "io.github.sandydunlop.markista.model",
                "io.github.sandydunlop.markista.utils"
        );
        String base = files.commonBase(packageNames);
        assertEquals("io.github.sandydunlop.markista", base);
    }

    @Test
    void createFile_withDirectorySet() throws IOException {
        Configuration.setFlattenDirectories(true);
		PackageNode sandydunlop = new PackageNode("io.github.sandydunlop");
		PackageNode github = new PackageNode("io.github");
		PackageNode markista = new PackageNode("io.github.sandydunlop.markista");
		PackageNode util = new PackageNode("io.github.sandydunlop.markista.util");
		PackageNode doclet = new PackageNode("io.github.sandydunlop.markista.doclet");
        module.addPackage(sandydunlop);
        module.addPackage(github);
        module.addPackage(markista);
        module.addPackage(util);
        module.addPackage(doclet);
        files.setFlattenedDirectories(module);
        Writer writer = files.createFile("MarkdownDoclet", "io.github.sandydunlop.markista.doclet");
        assertNotNull(writer);
    }

    @Test
    void createFile_withOutputNoSet() throws IOException {
        files = new FileUtils(module, null);
        Configuration.setFlattenDirectories(true);
		PackageNode sandydunlop = new PackageNode("io.github.sandydunlop");
		PackageNode github = new PackageNode("io.github");
		PackageNode markista = new PackageNode("io.github.sandydunlop.markista");
		PackageNode util = new PackageNode("io.github.sandydunlop.markista.util");
		PackageNode doclet = new PackageNode("io.github.sandydunlop.markista.doclet");
        module.addPackage(sandydunlop);
        module.addPackage(github);
        module.addPackage(markista);
        module.addPackage(util);
        module.addPackage(doclet);
        files.setFlattenedDirectories(module);
        Writer writer = files.createFile("MarkdownDoclet", "io.github.sandydunlop.markista.doclet");
        assertNotNull(writer);
        assertTrue(fileExists("build/md-docs/sandydunlop/markista/doclet/MarkdownDoclet.md"));
        deleteDirectory(new File("build/md-docs/sandydunlop"));
    }

    @Test
    void createModuleFile() throws IOException {
        files = new FileUtils(module, null);
        Configuration.setFlattenDirectories(true);
		PackageNode sandydunlop = new PackageNode("io.github.sandydunlop");
		PackageNode github = new PackageNode("io.github");
		PackageNode markista = new PackageNode("io.github.sandydunlop.markista");
		PackageNode util = new PackageNode("io.github.sandydunlop.markista.util");
		PackageNode doclet = new PackageNode("io.github.sandydunlop.markista.doclet");
        module.addPackage(sandydunlop);
        module.addPackage(github);
        module.addPackage(markista);
        module.addPackage(util);
        module.addPackage(doclet);
        files.setFlattenedDirectories(module);
        Writer writer = files.createModuleFile("io.github", "test.md");
        assertNotNull(writer);
        assertTrue(fileExists("build/md-docs/io.github/test.md"));
        deleteDirectory(new File("build/md-docs/io.github"));
    }

    @Test
    void joinPaths() {
        assertEquals("a/b", FileUtils.joinPaths("a","b"));
        assertEquals("a", FileUtils.joinPaths("a",""));
        assertEquals("b", FileUtils.joinPaths("","b"));
    }
}
