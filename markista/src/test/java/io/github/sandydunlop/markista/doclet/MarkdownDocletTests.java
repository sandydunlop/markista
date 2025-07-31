package io.github.sandydunlop.markista.doclet;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.io.File;

import com.sun.source.util.DocTreePath;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.util.FileUtils;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;
import javax.tools.DocumentationTool;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class MarkdownDocletTests {
    static final String BASE_DOC_PATH = "/tmp/doc/";
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

    boolean deleteDirectory(String directoryToBeDeleted) {
        if (directoryToBeDeleted.indexOf("tmp") == -1) return false;
        return deleteDirectory(new File(directoryToBeDeleted));
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
        // Nothing to do here
    }

    @BeforeEach
    void init() {
        files = new FileUtils(moduleNode, BASE_DOC_PATH);
    }

    boolean fileExists(String file) {
        return Files.exists(Paths.get(file));
    }

    boolean docFileExists(String file) {
        return Files.exists(Paths.get(FileUtils.joinPaths(BASE_DOC_PATH, file)));
    }

    @Test
    void run1_flatten() {
        String testOutputDir = BASE_DOC_PATH + "run1";
        deleteDirectory(testOutputDir);
        DocumentationTool systemDocumentationTool = ToolProvider.getSystemDocumentationTool();
        String[] args = new String[] {
            "-doclet", MarkdownDoclet.class.getName(),
            "-docletpath", "build/classes/java/main",
            "-d", testOutputDir,
            "-private",
            "-external",
            "-flatten",
            "-sourcepath", "src/main/java/",
            "-subpackages", "io.github.sandydunlop.markista",
        };
        DocumentationTool.DocumentationTask task = 
                systemDocumentationTool.getTask(null, null, null, 
                MarkdownDoclet.class, Arrays.asList(args), null);
        task.call();
        assertTrue(docFileExists("run1/index.md"));
        assertTrue(docFileExists("run1/constant-values.md"));
        assertTrue(docFileExists("run1/doclet/index.md"));
    }

    @Test
    void run2_noFlatten() {
        String testOutputDir = BASE_DOC_PATH + "run2";
        deleteDirectory(testOutputDir);
        DocumentationTool systemDocumentationTool = ToolProvider.getSystemDocumentationTool();
        String[] args = new String[] {
            "-doclet", MarkdownDoclet.class.getName(),
            "-docletpath", "build/classes/java/main", 
            "-d", testOutputDir, 
            "-private", 
            "-external",
            "-sourcepath", "src/main/java/",
            "-subpackages", "io.github.sandydunlop.markista",
        };
        DocumentationTool.DocumentationTask task = 
                systemDocumentationTool.getTask(null, null, null, 
                MarkdownDoclet.class, Arrays.asList(args), null);
        task.call();
        assertTrue(docFileExists("run2/index.md"));
        assertTrue(docFileExists("run2/constant-values.md"));
        assertTrue(docFileExists("run2/io/github/sandydunlop/markista/doclet/index.md"));
    }
}
