package io.github.sandydunlop.markista.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.tools.Diagnostic;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.PackageNode;
import jdk.javadoc.doclet.Reporter;

class ContextTests {
    static Context ctx;
    private Api api;
    private PackageNode pkg;
    private Reporter reporter;

	@BeforeAll
    static void initAll() {
		ctx = Context.getInstance();
    }

    @BeforeEach
    void setup() {
        reporter = mock(Reporter.class);
        ctx.setReporter(reporter);
        // Clear all context fields before each test
        ctx.setModuleName("");
        ctx.setPackageName("");
        ctx.setTypeName("");
        ctx.setMethodName("");
        ctx.setFieldName("");

        PackageNode pkg0 = new PackageNode("io.github.sandydunlop");
        pkg = new PackageNode("io.github.sandydunlop.test");
        api = new Api();
        api.addPackage(pkg0);
        api.addPackage(pkg);
        ctx.setApi(api);
    }

    @Test
    void testSetAndGetModuleName() {
        ctx.setModuleName("my.module");
        assertEquals("my.module",  ctx.getModuleName());
        // Setting module name resets package name
        ctx.setPackageName("pkg.name");
        ctx.setModuleName("new.module");
        assertEquals("",  ctx.getPackageName());
        assertEquals("new.module",  ctx.getModuleName());
    }

    @Test
    void testSetAndGetPackageName() {
        ctx.setPackageName("pkg.name");
        assertEquals("pkg.name",  ctx.getPackageName());
        // Setting package name resets type name
        ctx.setTypeName("TypeA");
        ctx.setPackageName("pkg.new");
        assertEquals("pkg.new",  ctx.getPackageName());
        assertEquals("",  ctx.getTypeName());
    }

    @Test
    void testSetAndGetTypeName() {
        ctx.setTypeName("TypeA");
        assertEquals("TypeA",  ctx.getTypeName());
    }

    @Test
    void testSetAndGetMethodName() {
        ctx.setMethodName("methodX");
        assertEquals("methodX",  ctx.getMethodName());
    }

    @Test
    void testSetAndGetFieldName() {
        ctx.setFieldName("fieldY");
        assertEquals("fieldY",  ctx.getFieldName());
    }

    @Test
    void testReportInfoCallsReporter() {
        String message = "Information";
        ctx.reportInfo(message);
        verify(reporter, times(1)).print(Diagnostic.Kind.NOTE, message);
    }

    @Test
    void testReportWarningIncludesLocation() {
        String message = "Warning message";
        // Setup context fields to produce location info
        ctx.setModuleName("mod1");
        ctx.setPackageName("pkg1");
        ctx.setTypeName("Type1");
        ctx.setMethodName("method1");
        ctx.setFieldName("field1");

        ctx.reportWarning(message);

        // Capture the message passed to print
        verify(reporter, times(1)).print(eq(Diagnostic.Kind.WARNING), contains(message));
        verify(reporter).print(eq(Diagnostic.Kind.WARNING), argThat(s -> s.contains("mod1") &&
                                                                    s.contains("pkg1") &&
                                                                    s.contains("Type1") &&
                                                                    s.contains("method1") &&
                                                                    s.contains("field1")));
    }

    @Test
    void testReportErrorIncludesLocationWhenNoFieldsIsEmpty() {
        String message = "Error message";
        // Clear all fields
        ctx.setModuleName("");
        ctx.setPackageName("");
        ctx.setTypeName("");
        ctx.setMethodName("");
        ctx.setFieldName("");

        ctx.reportError(message);

        verify(reporter).print(Diagnostic.Kind.ERROR, message);
    }

    @Test
    void testReportErrorIncludesLocationWhenFieldsSet() {
        String message = "Error found";
        ctx.setModuleName("moduleA");
        ctx.setTypeName("TypeB");
        ctx.setMethodName("methodC");

        ctx.reportError(message);

        verify(reporter).print(eq(Diagnostic.Kind.ERROR), argThat(s -> s.contains(message) &&
                                                                   s.contains("moduleA") &&
                                                                   s.contains("TypeB") &&
                                                                   s.contains("methodC")));
    }

    boolean fileExists(String file) {
        return Files.exists(Paths.get(file));
    }

    @Test
    void buildContainingDirPath() {
        String outputDir = "/tmp/markista/dirpath/";
        ctx.setOutputDirectory(outputDir);
        Configuration.setFlattenDirectories(true);

        ctx.setModuleName("");
        ctx.setPackageName(pkg.getName());
        File file = ctx.getDirectory();
        assertEquals(outputDir + "test", file.getAbsolutePath());
    }

    @Test
    void createFile_namedModule() throws IOException {
        String outputDir = "/tmp/markista/dirpath/";
        ctx.setOutputDirectory(outputDir);
        Configuration.setFlattenDirectories(true);

        ctx.setModuleName("markista");
        ctx.setPackageName(pkg.getName());

        OutputStreamWriter w = (OutputStreamWriter)ctx.createFile();
        w.write("test createFile 1");
        w.flush();
        w.close();

        BufferedReader reader = new BufferedReader(new FileReader(outputDir + "markista/test/index.md"));
        String line = reader.readLine();
        reader.close();
        assertTrue(line.contains("test createFile 1"));
    }

    @Test
    void createFile_unnamedModule() throws IOException {
        String outputDir = "/tmp/markista/dirpath/";
        ctx.setOutputDirectory(outputDir);
        Configuration.setFlattenDirectories(true);

        ctx.setModuleName("");
        ctx.setPackageName(pkg.getName());

        OutputStreamWriter w = (OutputStreamWriter)ctx.createFile();
        w.write("test createFile 1");
        w.flush();
        w.close();

        BufferedReader reader = new BufferedReader(new FileReader(outputDir + "test/index.md"));
        String line = reader.readLine();
        reader.close();
        assertTrue(line.contains("test createFile 1"));
    }

    @Test
    void createModuleFile_namedModule() throws IOException {
        String outputDir = "/tmp/markista/dirpath/";
        ctx.setOutputDirectory(outputDir);
        Configuration.setFlattenDirectories(true);

        ctx.setModuleName("markista");
        ctx.setPackageName("");

        OutputStreamWriter w = (OutputStreamWriter)ctx.createModuleFile("test.md");
        w.write("test createFile module 1");
        w.flush();
        w.close();

        BufferedReader reader = new BufferedReader(new FileReader(outputDir + "markista/test.md"));
        String line = reader.readLine();
        reader.close();
        assertTrue(line.contains("test createFile module 1"));
    }

    @Test
    void createModuleFile_unnamedModule() throws IOException {
        String outputDir = "/tmp/markista/dirpath/";
        ctx.setOutputDirectory(outputDir);
        Configuration.setFlattenDirectories(true);

        ctx.setModuleName("");
        ctx.setPackageName("");

        OutputStreamWriter w = (OutputStreamWriter)ctx.createModuleFile("test.md");
        w.write("test createFile module 2");
        w.flush();
        w.close();

        BufferedReader reader = new BufferedReader(new FileReader(outputDir + "test.md"));
        String line = reader.readLine();
        reader.close();
        assertTrue(line.contains("test createFile module 2"));
    }
}