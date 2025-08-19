package io.github.sandydunlop.markista.core;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.PackageNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.lang.model.element.Element;

import jdk.javadoc.doclet.Reporter;

import com.sun.source.util.DocTreePath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextTests {
    static Context ctx;
    private Api api;
    private PackageNode pkg;

    StringWriter stringWriter = new StringWriter();

    static TestReporter reporter;

    @BeforeAll
    static void initAll() {
		ctx = Context.getInstance();
    }

    @BeforeEach
    void setup() {
        reporter = new TestReporter();
        reporter.stringWriter = new StringWriter();
        ctx.setReporter(reporter);
        ctx.setModuleName("");

        PackageNode pkg0 = new PackageNode("io.github.sandydunlop");
        pkg = new PackageNode("io.github.sandydunlop.test");
        api = new Api("Test API");
        api.addPackage(pkg0);
        api.addPackage(pkg);
        ctx.setApi(api);

        context = Context.getInstance();
        context.setApi(api);
        context.setOutputDirectory(null);
        context.setModuleName("");
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
        reporter.stringWriter = new StringWriter();
        ctx.reportInfo(message);
        assertTrue(reporter.stringWriter.toString().contains(message));
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

        reporter.stringWriter = new StringWriter();
        ctx.reportWarning(message);

        // Capture the message passed to print
        assertTrue(reporter.stringWriter.toString().contains("mod1"));
        assertTrue(reporter.stringWriter.toString().contains("pkg1"));
        assertTrue(reporter.stringWriter.toString().contains("Type1"));
        assertTrue(reporter.stringWriter.toString().contains("method1"));
        assertTrue(reporter.stringWriter.toString().contains("field1"));
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

        reporter.stringWriter = new StringWriter();
        ctx.reportError(message);

        assertTrue(reporter.stringWriter.toString().contains("message"));
    }

    @Test
    void testReportErrorIncludesLocationWhenFieldsSet() {
        String message = "Error found";
        ctx.setModuleName("moduleA");
        ctx.setTypeName("TypeB");
        ctx.setMethodName("methodC");

        reporter.stringWriter = new StringWriter();
        ctx.reportError(message);

        assertTrue(reporter.stringWriter.toString().contains("moduleA"));
        assertTrue(reporter.stringWriter.toString().contains("TypeB"));
        assertTrue(reporter.stringWriter.toString().contains("methodC"));
    }

    boolean fileExists(String file) {
        return Files.exists(Paths.get(file));
    }

    @Test
    void buildContainingDirPath() {
        String outputDir = "/tmp/markista/dirpath/";
        Configuration.setFlattenPackages(true);
        ctx.setOutputDirectory(outputDir);
        ctx.setApi(api);

        ctx.setModuleName("");
        ctx.setPackageName(pkg.getQualifiedName());
        File file = ctx.getPackageDirectory();
        assertEquals(outputDir + "sandydunlop/test", file.getAbsolutePath());
    }

    @Disabled("Needs to be mocked")
    @Test
    void createFile_namedModule() throws IOException {
        String outputDir = "/tmp/markista/dirpath/";
        ctx.setOutputDirectory(outputDir);
        Configuration.setFlattenPackages(true);

        ctx.setModuleName("markista");
        ctx.setPackageName(pkg.getQualifiedName());

        OutputStreamWriter w = (OutputStreamWriter)ctx.createFileInPackage();
        w.write("test createFile 1");
        w.flush();
        w.close();

        BufferedReader reader = new BufferedReader(new FileReader(outputDir + "markista/test/index.md"));
        String line = reader.readLine();
        reader.close();
        assertTrue(line.contains("test createFile 1"));
    }

    @Disabled("Needs to be mocked")
    @Test
    void createFile_unnamedModule() throws IOException {
        String outputDir = "/tmp/markista/dirpath/";
        ctx.setOutputDirectory(outputDir);
        Configuration.setFlattenPackages(true);

        ctx.setModuleName("");
        ctx.setPackageName(pkg.getQualifiedName());

        OutputStreamWriter w = (OutputStreamWriter)ctx.createFileInPackage();
        w.write("test createFile 1");
        w.flush();
        w.close();

        BufferedReader reader = new BufferedReader(new FileReader(outputDir + "test/index.md"));
        String line = reader.readLine();
        reader.close();
        assertTrue(line.contains("test createFile 1"));
    }

    @Test
    void createModuleFile_namedModule() throws InvalidPathException, IOException {
        String outputDir = "/tmp/markista/dirpath/";
        ctx.setOutputDirectory(outputDir);
        Configuration.setFlattenPackages(true);

        ctx.setModuleName("markista");
        ctx.setPackageName("");

        OutputStreamWriter w = (OutputStreamWriter)ctx.createFileInModule("test.md");
        w.write("test createFile module 1");
        w.flush();
        w.close();

        BufferedReader reader = new BufferedReader(new FileReader(outputDir + "markista/test.md"));
        String line = reader.readLine();
        reader.close();
        assertTrue(line.contains("test createFile module 1"));
    }

    @Test
    void createModuleFile_unnamedModule() throws InvalidPathException, IOException {
        String outputDir = "/tmp/markista/dirpath/";
        ctx.setOutputDirectory(outputDir);
        Configuration.setFlattenPackages(true);

        ctx.setModuleName("");
        ctx.setPackageName("");

        OutputStreamWriter w = (OutputStreamWriter)ctx.createFileInModule("test.md");
        w.write("test createFile module 2");
        w.flush();
        w.close();

        BufferedReader reader = new BufferedReader(new FileReader(outputDir + "test.md"));
        String line = reader.readLine();
        reader.close();
        assertTrue(line.contains("test createFile module 2"));
    }

    private Context context;

    @Test
    void testSetAndGetOutputDirectory() {
        context.setOutputDirectory("out/dir");
        assertEquals("out/dir", context.getOutputDirectory());

        context.setOutputDirectory(null);
        assertEquals("", context.getOutputDirectory());
    }

    @Test
    void testSetAndGetReporter() {
        Reporter reporter2 = Mockito.mock(Reporter.class);
        context.setReporter(reporter2);
        assertSame(reporter2, context.getReporter());
    }

    @Test
    void testSetAndGetModulePackageTypeMethodFieldNames() {
        context.setModuleName("mod1");
        assertEquals("mod1", context.getModuleName());
        context.setPackageName("pkg1");
        assertEquals("pkg1", context.getPackageName());
        context.setTypeName("typ1");
        assertEquals("typ1", context.getTypeName());
        context.setMethodName("meth1");
        assertEquals("meth1", context.getMethodName());
        context.setFieldName("field1");
        assertEquals("field1", context.getFieldName());

        // Setting null resets to empty string
        context.setModuleName(null);
        assertEquals("", context.getModuleName());
    }

    @Test
    void testLocationStringFormatting() throws Exception {
        // Reflection to invoke private location()
        var locationMethod = Context.class.getDeclaredMethod("location");
        locationMethod.setAccessible(true);

        // No location info returns empty string
        context.setModuleName("");
        context.setPackageName("");
        context.setTypeName("");
        context.setMethodName("");
        context.setFieldName("");
        assertEquals("", locationMethod.invoke(context));

        // Set some location info
        context.setModuleName("M");
        context.setPackageName("P");
        context.setTypeName("T");
        context.setMethodName("Mth");
        context.setFieldName("Fld");

        String locStr = (String) locationMethod.invoke(context);

        assertTrue(locStr.contains("[ Module] M"));
        assertTrue(locStr.contains("[Package] P"));
        assertTrue(locStr.contains("[   Type] T"));
        assertTrue(locStr.contains("[ Method] Mth"));
        assertTrue(locStr.contains("[  Field] Fld"));
    }

    @Test
    void testReportMethodsCallReporterWithCorrectKindAndMessage() {
        context.setModuleName("mod");
        context.setPackageName("pkg");
        context.setTypeName("typ");
        context.setMethodName("meth");
        context.setFieldName("fld");

        reporter.stringWriter = new StringWriter();
        context.reportInfo("infoMsg");
        assertTrue(reporter.stringWriter.toString().contains("infoMsg"));

        reporter.stringWriter = new StringWriter();
        context.reportWarning("warnMsg");
        assertTrue(reporter.stringWriter.toString().contains("warnMsg"));
        assertTrue(reporter.stringWriter.toString().contains("Location"));

        reporter.stringWriter = new StringWriter();
        context.reportError("errorMsg");
        assertTrue(reporter.stringWriter.toString().contains("errorMsg"));
        assertTrue(reporter.stringWriter.toString().contains("Location"));
    }

    @Test
    void testGetPackageDirectoryDefaultOutputDir() {
        context.setOutputDirectory(null);
        context.setModuleName("modX");
        context.setPackageName("com.example.test");

        // Assuming Configuration.getFlattenModules() and getFlattenPackages() are false
        Configuration.setFlattenModules(false);
        Configuration.setFlattenPackages(false);

        File dir = context.getPackageDirectory();

        assertTrue(dir.getPath().endsWith("modX/com/example/test") || dir.getPath().endsWith("modX\\com\\example\\test"));
        // The base directory should default to "."
        assertTrue(dir.getAbsolutePath().contains(new File(".").getAbsolutePath()));
    }

    @Disabled("FILESYSTEM")
    @Test
    void testCreateFileInPackageCreatesDirectoriesAndFile() throws IOException {
        // Use a temp directory
        Path tempDir = Files.createTempDirectory("ctxTest");
        context.setOutputDirectory(tempDir.toString());
        context.setModuleName("mod");
        context.setPackageName("com.example.pkg");
        context.setTypeName("MyType");

        Writer writer = context.createFileInPackage();
        assertNotNull(writer);
        writer.close();

        // Check file exists
        File expectedFile = tempDir.resolve("mod").resolve("com").resolve("example").resolve("pkg").resolve("MyType.md").toFile();
        assertTrue(expectedFile.exists());

        // Clean up
        expectedFile.delete();
        expectedFile.getParentFile().delete();
        expectedFile.getParentFile().getParentFile().delete();
    }

    @Test
    void testCreateFileInModuleCreatesDirectoriesAndFile() throws InvalidPathException, IOException {
        Path tempDir = Files.createTempDirectory("ctxModTest");
        context.setOutputDirectory(tempDir.toString());
        context.setModuleName("mymodule");

        Writer writer = context.createFileInModule("moduleDoc.md");
        assertNotNull(writer);
        writer.close();

        File expectedFile = tempDir.resolve("mymodule").resolve("moduleDoc.md").toFile();
        assertTrue(expectedFile.exists());

        // Cleanup
        expectedFile.delete();
        expectedFile.getParentFile().delete();
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
    
    static List<Object> typeReferenceProvider() {
        return List.of(
            new Object[] { "java.lang.String", "String"},
            new Object[] { "java.util.List", "List"},
            new Object[] { "java.util.List<java.lang.String>", "List<String>"},
            new Object[] { "java.util.HashMap<java.lang.String, java.lang.String[]>", "HashMap<String, String[]>"},
            new Object[] { "java.util.List<? extends java.lang.String>", "List<? extends String>"},
            new Object[] { "java.lang.String[]", "String[]"}
        );
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("typeReferenceProvider")
	void link_to_text(String names, String expected) {
        String simplified = Context.NameSimplifier.simplifyNames(names);
        assertEquals(expected, simplified);
    }

    @Test
    void nameSimplifier() {
        String name = "io.github.sandydunlop.markista.model.Node";
        String simplified = Context.NameSimplifier.simplifyNames(name);
        assertEquals("Node", simplified);
    }
}