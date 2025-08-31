package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.core.Configuration;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.Link;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.module.ModuleDescriptor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.lang.model.element.Element;

import jdk.javadoc.doclet.Reporter;

import com.sun.source.util.DocTreePath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkResolverTests {
    private static final String JAVA_24_URL = "https://docs.oracle.com/en/java/javase/24/docs/api/";
	private static Context ctx;
	private Api api;
    private ModuleNode module;
    private PackageNode model;
    private PackageNode doclet;
	private PackageNode markista;
	private PackageNode util;
    private ClassNode node;
    private ClassNode markdownDoclet;

    Link link;
    private static final String ORIGIN = "com.example";
    private static final String TARGET = "com.example.MyClass";
    private JarFile mockJarFile;

    TestReporter reporter;

	@BeforeAll
    static void initAll() {
		ctx =  Context.getInstance();
    }

    @BeforeEach
    void init() {
        reporter = new TestReporter();
        reporter.stringWriter = new StringWriter();
		ctx.setReporter(reporter);

        api = new Api("Test API");
        api.addPackage(new PackageNode("io.github.sandydunlop"));
        markista = new PackageNode("io.github.sandydunlop.markista");
		util = new PackageNode("io.github.sandydunlop.markista.util");
		doclet = new PackageNode("io.github.sandydunlop.markista.doclet");
		model = new PackageNode("io.github.sandydunlop.markista.model");
		api.addPackage(markista);
		api.addPackage(util);
		api.addPackage(doclet);
		api.addPackage(model);
		api.addType(new ClassNode("LinkResolver", util.getName()));
		api.addType(new ClassNode("MarkdownDoclet", doclet.getName()));
		api.addType(new ClassNode("MarkdownDoclet.Option", doclet.getName()));

        module = new ModuleNode("markista");
		api.addModule(module);
		module.addPackage(markista);
		module.addPackage(util);
		module.addPackage(doclet);
		module.addPackage(model);
		markista.setModuleName(module.getName());
		util.setModuleName(module.getName());
		doclet.setModuleName(module.getName());
		model.setModuleName(module.getName());

        node = new ClassNode("Node", model.getName());
        model.addType(node);
        api.addType(node);

        markdownDoclet = new ClassNode("MarkdownDoclet", doclet.getName());
        model.addType(markdownDoclet);

		LinkResolver.init(api, ctx);
        LinkResolver.addStandardModuleUrl("java.base", "https://docs.oracle.com/en/java/javase/24/docs/api/java.base", ".html");
		LinkResolver.setFlattenedDirectories(null);
        LinkResolver.siblingClassNames = new java.util.HashSet<>();
		ctx.setModuleName("markista");
        ctx.setPackageName("");

        link = new Link();
        link.setOriginPackage(ORIGIN);
        link.setTarget(TARGET);
        link.setKind(Link.Kind.UNKNOWN);
    }

	@Test
	void addStandardModuleUrl() {
		LinkResolver.init(api, ctx);
		LinkResolver.setFlattenedDirectories(null);

		String moduleName = "jdk.javadoc";
        LinkResolver.addStandardModuleUrl(moduleName, JAVA_24_URL + moduleName, ".html");

		Link ref = new Link();
		ref.setTarget("jdk.javadoc.doclet.Doclet");
		assertTrue(LinkResolver.resolveStandardPackageOrType(ref));

		assertEquals(Link.Scope.STANDARD, ref.getScope());
		assertEquals(Link.Kind.URL, ref.getKind());
		assertEquals("https://docs.oracle.com/en/java/javase/24/docs/api/jdk.javadoc/jdk/javadoc/doclet/Doclet.html", ref.getUri());
    }

    @Test
    void qualifyType() {
        String[] qualified = LinkResolver.qualifyType("Node");
        String toPackageName = qualified[0];
        assertEquals("io.github.sandydunlop.markista.model", toPackageName);
    }

	@Test
	void relativize_fromNoLevel() {
		String path = LinkResolver.relativize("", "io.github.sandydunlop.markista.model.Node");
		assertEquals("io/github/sandydunlop/markista/model/Node", path);
	}

	@Test
	void relativize_squashed_fromNoLevel() {
		LinkResolver.setFlattenedDirectories("io.github.sandydunlop.markista");
		String path = LinkResolver.relativize("", "io.github.sandydunlop.markista.doclet");
		assertEquals("doclet", path);
	}

	@Test
	void relativize_squashed_toSameLevel() {
		LinkResolver.setFlattenedDirectories("io.github.sandydunlop.markista");
		String path = LinkResolver.relativize("io.github.sandydunlop.markista.util", "io.github.sandydunlop.markista.doclet");
		assertEquals("../doclet", path);
	}

	@Test
	void relativize_toParentLevel() {
		String path = LinkResolver.relativize("io.github.sandydunlop.markista.util", "io.github.sandydunlop.markista");
		assertEquals("..", path);
	}

	@Test
	void relativize_toSameLevel() {
		String path = LinkResolver.relativize("io.github.sandydunlop.markista.util", "io.github.sandydunlop.markista.doclet");
		assertEquals("../doclet", path);
	}

	@Test
	void resolve_empty() {
		link = LinkResolver.resolve(Link.to(""));
		assertEquals(Link.Kind.UNKNOWN, link.getKind());
		assertEquals("", link.getUri());
	}

	@Test
	void resolve_module() {
		link = LinkResolver.resolve(Link.to("java.base"));
		assertEquals(Link.Kind.URL, link.getKind());
		assertEquals("https://docs.oracle.com/en/java/javase/24/docs/api/java.base/module-summary.html", link.getUri());
	}

	@Test
	void resolve_packageFromModule() {
		LinkResolver.setFlattenedDirectories("io.github.sandydunlop.markista");
		link = LinkResolver.resolve(Link.to("io.github.sandydunlop.markista.doclet"));
		assertEquals("doclet", link.getUri());
	}

	@Test
	void resolve_primitive() {
		link = Link.to("boolean").fromPackage("io.github.sandydunlop.markista.util");
		link = LinkResolver.resolve(link);
		assertEquals(Link.Kind.PRIMITIVE, link.getKind());
	}

	@Test
	void resolve_qualifiedPackage_prevLevel() {
		link = Link.to("io.github.sandydunlop.markista").fromPackage("io.github.sandydunlop.markista.util");
		link = LinkResolver.resolve(link);
		assertEquals("..", link.getUri());
	}

	@Test
	void resolve_qualifiedStandardClass() {
		link = LinkResolver.resolve(Link.to("java.util.List"));
		link = LinkResolver.resolve(link);
		assertEquals("https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/util/List.html", link.getUri());
	}

	@Test
	void resolve_qualifiedClass_noLevel() {
		link = LinkResolver.resolve(Link.to("io.github.sandydunlop.markista.model.Node"));
		link = LinkResolver.resolve(link);
		assertEquals("io/github/sandydunlop/markista/model/Node", link.getUri());
	}

	@Test
	void resolve_undefinedType() {
		// This will show a warning in the test output
		link = LinkResolver.resolve(Link.to("Coso"));
		assertEquals("", link.getUri());
		assertEquals(Link.Kind.UNKNOWN, link.getKind());
	}

	@Test
	void resolve_unqualifiedPackage_nextLevelPackage() {
		link = Link.to("doclet").fromPackage("io.github.sandydunlop.markista");
		link = LinkResolver.resolve(link);
		assertEquals("doclet", link.getUri());
	}

	@Test
	void resolve_unqualifiedPackage_sameLevelPackage() {
		link = Link.to("doclet").fromPackage("io.github.sandydunlop.markista.util");
		link = LinkResolver.resolve(link);
		assertEquals("../doclet", link.getUri());
	}

	@Test
	void resolve_unqualifiedClass_sameLevel() {
		link = Link.to("MarkdownDoclet").fromPackage("io.github.sandydunlop.markista.util");
		link = LinkResolver.resolve(link);
		assertEquals("../doclet/MarkdownDoclet", link.getUri());
	}

	@Test
	void resolve_unqualifiedPackage_prevLevel() {
		link = Link.to("markista").fromPackage("io.github.sandydunlop.markista.util");
		link = LinkResolver.resolve(link);
		assertEquals("..", link.getUri());
	}

	@Test
	void resolve_void() {
		link = LinkResolver.resolve(Link.to("void"));
		assertEquals(Link.Kind.VOID, link.getKind());
		assertEquals("", link.getUri());
	}

	@Test
	void resolveModule() {
		link = Link.to("markista/");
		link = LinkResolver.resolve(link);
		assertEquals(Link.Kind.MODULE, link.getKind());
		assertNotEquals("", link.getUri());
		assertNotEquals(null, link.getUri());
	}

    @Disabled("CONTEXT")
    @Test
    void resolve_NullOrigin_SetsOriginFromContext() {
        Link linkWithoutOrigin = new Link();
        linkWithoutOrigin.setTarget("java.lang.String");
        linkWithoutOrigin.setKind(Link.Kind.UNKNOWN);

        Context mockContext = mock(Context.class);
        when(mockContext.getPackageName()).thenReturn("default.pkg");

        Context originalCtx = LinkResolver.ctx;
        LinkResolver.ctx = mockContext;

        Link resolvedLink = LinkResolver.resolve(linkWithoutOrigin);
        assertEquals("default.pkg", resolvedLink.getOriginPackage());

        LinkResolver.ctx = originalCtx;
    }

    @Test
    void resolve_EmptyTarget_ReturnsUnchanged() {
        link.setTarget("");
        Link resolved = LinkResolver.resolve(link);
        // Target empty, should return quickly
        assertEquals("", resolved.getTarget());
        assertFalse(resolved.isResolved());
    }

    @Test
    void resolve_UnsupportedKind_ReturnsImmediately() {
        link.setKind(Link.Kind.UNSUPPORTED);

        // We simulate resolveUnsupported returns the same link with unsupported kind
        Link resolved = LinkResolver.resolve(link);
        assertEquals(Link.Kind.UNSUPPORTED, resolved.getKind());
    }

    @Test
    void resolve_TargetEndsWithSlash_SetsKindModule() {
        module = new ModuleNode("markista");
        api.addModule(module);
        LinkResolver.init(api, ctx);
        link.setTarget("markista/");
        Link resolved = LinkResolver.resolve(link);
        assertEquals(Link.Kind.MODULE, resolved.getKind());
    }

    @Test
    void resolve_PrimitiveOrVoid_ResolvesSuccessfully() {
        link.setTarget("void");
        link.setKind(Link.Kind.UNKNOWN);

        Link spyLink = spy(link);
        // will call real resolvePrimitiveOrVoid - simulate it resolving the link
        LinkResolver.resolvePrimitiveOrVoid(spyLink);
        Link resolved = LinkResolver.resolve(spyLink);
        assertTrue(resolved.isResolved());
    }

    @Test
    void tryResolvePrimitiveOrVoid_ReturnsTrueIfResolved() {
        Link primitiveLink = new Link();
        primitiveLink.setKind(Link.Kind.UNKNOWN);
        primitiveLink.setTarget("int");

        boolean result = LinkResolver.resolvePrimitiveOrVoid(primitiveLink);
        assertTrue(result);
    }

    @Test
    void tryResolveStandardPackageOrType_ReturnsFalseIfNotResolved() {
        link = new Link();
        link.setKind(Link.Kind.TYPE);
        link.setTarget("unknown.Target");

        boolean result = LinkResolver.resolveStandardPackageOrType(link);
        // Depending on data, resolveStandardPackageOrType may not resolve, result can be false
        assertFalse(result); // just assert the method runs without error
    }

    @Test
    void tryResolveLocalPackageOrType_ReturnsFalseIfNotResolved() {
        link = new Link();
        link.setKind(Link.Kind.PACKAGE);
        link.setTarget("local.pkg");

        boolean result = LinkResolver.resolveLocalPackageOrType(link);
        assertFalse(result); // just assert the method runs without error
    }

    @Test
    void tryResolveLocalModule_ReturnsFalseIfNotResolved() {
        link = new Link();
        link.setKind(Link.Kind.MODULE);
        link.setTarget("local.module");

        boolean result = LinkResolver.resolveLocalModule(link);
        assertFalse(result); // just assert the method runs without error
    }

    @Test
    void tryResolveSiblingModule_ReturnsFalseIfNotResolved() {
        link = new Link();
        link.setKind(Link.Kind.MODULE);
        link.setTarget("sibling.module");

        boolean result = LinkResolver.resolveSiblingModule(link);
        assertFalse(result); // just assert the method runs without error
    }

    @Test
    void tryResolveSiblingType_ReturnsFalseIfNotResolved() {
        link = new Link();
        link.setKind(Link.Kind.TYPE);
        link.setTarget("sibling.type");

        boolean result = LinkResolver.resolveSiblingType(link);
        assertFalse(result);
    }

    @BeforeEach
    void setUp() {
        mockJarFile = Mockito.mock(JarFile.class);
        // Assuming Configuration is a static class with a static method getListExternal
        Configuration.setLinkExternal("");
	}

	// Helper method to create a mock enumeration of JarEntries
    private Enumeration<JarEntry> createMockEnumeration(JarEntry... entries) {
        return Collections.enumeration(java.util.Arrays.asList(entries));
    }

    // Helper method to create a mock JarEntry
    private JarEntry createMockJarEntry(String name) {
        JarEntry entry = Mockito.mock(JarEntry.class);
        Mockito.when(entry.getName()).thenReturn(name);
        return entry;
    }

    @Test
    void testProcessJarFileWithModuleInfoAndClasses() throws IOException {
        // Arrange
        String moduleName = "com.example.module";
        String className1 = "com.example.module.MyClass";
        String className2 = "com.example.module.AnotherClass";

        // Create a mock ModuleDescriptor object
        ModuleDescriptor mockDescriptor = Mockito.mock(ModuleDescriptor.class);
        Mockito.when(mockDescriptor.name()).thenReturn(moduleName);

        // Mock the JarFile entries
        JarEntry moduleInfoEntry = createMockJarEntry("module-info.class");
        JarEntry class1Entry = createMockJarEntry("com/example/module/MyClass.class");
        JarEntry class2Entry = createMockJarEntry("com/example/module/AnotherClass.class");
        Mockito.when(mockJarFile.entries()).thenReturn(createMockEnumeration(moduleInfoEntry, class1Entry, class2Entry));

        // Use a dummy InputStream for the module-info entry
        Mockito.when(mockJarFile.getInputStream(moduleInfoEntry)).thenReturn(new ByteArrayInputStream(new byte[0]));

        // Make the module "external"
        Configuration.setLinkExternal(moduleName);

        // Act & Assert
        // Use Mockito.mockStatic to mock the static ModuleDescriptor.read() method
        try (MockedStatic<ModuleDescriptor> mockedStatic = Mockito.mockStatic(ModuleDescriptor.class)) {
            // Corrected line: specify the InputStream.class to resolve ambiguity
            mockedStatic.when(() -> ModuleDescriptor.read(Mockito.any(InputStream.class))).thenReturn(mockDescriptor);

            // Call the method under test
            LinkResolver.processJarFile(mockJarFile);
        }

        // Make the module "external"
        Configuration.setLinkExternal(moduleName);

		// Assert
        assertTrue(LinkResolver.siblingClassNames.contains(className1));
        assertTrue(LinkResolver.siblingClassNames.contains(className2));
    }

    @Test
    void testProcessJarFileWithNonExternalModule() throws IOException {
        // Arrange
        String moduleName = "com.example.nonexternal";

        // Create a mock ModuleDescriptor object
        ModuleDescriptor mockDescriptor = Mockito.mock(ModuleDescriptor.class);
        Mockito.when(mockDescriptor.name()).thenReturn(moduleName);

        // Create mock JarEntries
        JarEntry moduleInfoEntry = createMockJarEntry("module-info.class");

        Mockito.when(mockJarFile.entries()).thenReturn(createMockEnumeration(moduleInfoEntry));

        // Use a dummy InputStream for the module-info entry
        Mockito.when(mockJarFile.getInputStream(moduleInfoEntry)).thenReturn(new ByteArrayInputStream(new byte[0]));

        // Make the module "external"
        Configuration.setLinkExternal(moduleName);

        // Act & Assert
        // Use Mockito.mockStatic to mock the static ModuleDescriptor.read() method
        try (MockedStatic<ModuleDescriptor> mockedStatic = Mockito.mockStatic(ModuleDescriptor.class)) {
            // Corrected line: specify the InputStream.class to resolve ambiguity
            mockedStatic.when(() -> ModuleDescriptor.read(Mockito.any(InputStream.class))).thenReturn(mockDescriptor);

            // Call the method under test
            LinkResolver.processJarFile(mockJarFile);
        }

        // Assert
        assertEquals(moduleName, LinkResolver.siblingModuleName);
        assertTrue(LinkResolver.siblingClassNames.isEmpty()); // Class should not be mapped
    }

    @Test
    void testProcessJarFileWithNoClassFiles() throws IOException {
        // Arrange
        String moduleName = "com.example.empty";

        // Create mock JarEntries with only a module-info.class
        JarEntry moduleInfoEntry = createMockJarEntry("module-info.class");
        ModuleDescriptor moduleDescriptor = ModuleDescriptor.newModule(moduleName).build();
        ByteArrayInputStream moduleInfoStream = new ByteArrayInputStream(moduleDescriptor.toString().getBytes());

        // Stub the behavior of the mock JarFile
        Mockito.when(mockJarFile.entries()).thenReturn(createMockEnumeration(moduleInfoEntry));
        Mockito.when(mockJarFile.getInputStream(moduleInfoEntry)).thenReturn(moduleInfoStream);

        // Make the module "external"
        Configuration.setLinkExternal(moduleName);

        // Create a mock ModuleDescriptor object
        ModuleDescriptor mockDescriptor = Mockito.mock(ModuleDescriptor.class);
        Mockito.when(mockDescriptor.name()).thenReturn(moduleName);
        // Use a dummy InputStream for the module-info entry
        Mockito.when(mockJarFile.getInputStream(moduleInfoEntry)).thenReturn(new ByteArrayInputStream(new byte[0]));

        // Make the module "external"
        Configuration.setLinkExternal(moduleName);

        // Act & Assert
        // Use Mockito.mockStatic to mock the static ModuleDescriptor.read() method
        try (MockedStatic<ModuleDescriptor> mockedStatic = Mockito.mockStatic(ModuleDescriptor.class)) {
            // Corrected line: specify the InputStream.class to resolve ambiguity
            mockedStatic.when(() -> ModuleDescriptor.read(Mockito.any(InputStream.class))).thenReturn(mockDescriptor);

            // Call the method under test
            LinkResolver.processJarFile(mockJarFile);
        }

        // Make the module "external"
        Configuration.setLinkExternal(moduleName);

        // Assert
        assertEquals(moduleName, LinkResolver.siblingModuleName);
        assertTrue(LinkResolver.siblingClassNames.isEmpty());
    }

    @Test
    void testProcessJarFileWithWindowsSeparators() throws IOException {
        // Arrange
        String moduleName = "com.example.windows";
        String className = "com.example.windows.MyClass";

        // Create mock JarEntries with Windows-style path separators
        JarEntry moduleInfoEntry = createMockJarEntry("module-info.class");
        JarEntry classEntry = createMockJarEntry("com\\example\\windows\\MyClass.class");

        ModuleDescriptor mockDescriptor = Mockito.mock(ModuleDescriptor.class);
        Mockito.when(mockDescriptor.name()).thenReturn(moduleName);

		Mockito.when(mockJarFile.entries()).thenReturn(createMockEnumeration(moduleInfoEntry, classEntry));

        // Use a dummy InputStream for the module-info entry
        Mockito.when(mockJarFile.getInputStream(moduleInfoEntry)).thenReturn(new ByteArrayInputStream(new byte[0]));

        // Make the module "external"
        Configuration.setLinkExternal(moduleName);

        // Act & Assert
        // Use Mockito.mockStatic to mock the static ModuleDescriptor.read() method
        try (MockedStatic<ModuleDescriptor> mockedStatic = Mockito.mockStatic(ModuleDescriptor.class)) {
            // Corrected line: specify the InputStream.class to resolve ambiguity
            mockedStatic.when(() -> ModuleDescriptor.read(Mockito.any(InputStream.class))).thenReturn(mockDescriptor);

            // Call the method under test
            LinkResolver.processJarFile(mockJarFile);
        }

        // Make the module "external"
        Configuration.setLinkExternal(moduleName);


        // Assert
        assertTrue(LinkResolver.siblingClassNames.contains(className));
        assertEquals(moduleName, LinkResolver.siblingModuleName);
    }

    @Test
    void testProcessJarFileWithIOException() throws IOException {
        // Arrange
        JarEntry moduleInfoEntry = createMockJarEntry("module-info.class");

        // Stub the behavior of the mock JarFile to throw an IOException
        Mockito.when(mockJarFile.entries()).thenReturn(createMockEnumeration(moduleInfoEntry));
        Mockito.when(mockJarFile.getInputStream(moduleInfoEntry)).thenThrow(new IOException("Test exception"));

        // Act & Assert
        assertThrows(IOException.class, () -> LinkResolver.processJarFile(mockJarFile));
    }

    void setup2() {
        LinkResolver.classToModule = new java.util.HashMap<>();
        LinkResolver.siblingModules = new java.util.ArrayList<>();
        LinkResolver.siblingClassNames = new HashSet<>();
        LinkResolver.siblingModuleName = "";
        Configuration.setModulePaths("");
        Configuration.setLinkExternal("");
    }

    @Test
    void testProcessDirectory_processesClassFiles() throws IOException {
        Path tempDir = Files.createTempDirectory("testProcessDir");

        // Create dummy class file
        File dummyClass = tempDir.resolve("Dummy.class").toFile();
        dummyClass.createNewFile();

        URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { tempDir.toUri().toURL() });

        MockedStatic<Files> fakeFiles = Mockito.mockStatic(Files.class, Mockito.CALLS_REAL_METHODS);

        Path classFile = dummyClass.toPath();
        List<Path> list = List.of(classFile);
        File f = tempDir.toFile();

        try (MockedStatic<LinkResolver> mockStatic = Mockito.mockStatic(LinkResolver.class, Mockito.CALLS_REAL_METHODS)) {
            mockStatic.when(() -> LinkResolver.processDirectory(any())).thenCallRealMethod();
            mockStatic.when(() -> LinkResolver.processClassFile(any(), any(), any())).thenAnswer((Answer<Void>) _ -> null);
            fakeFiles.when(() -> Files.walk(f.toPath())).thenReturn(list.stream());

            LinkResolver.processDirectory(f);
            mockStatic.verify(() -> LinkResolver.processClassFile(any(), any(), any()), times(1));
        } finally {
            dummyClass.delete();
            tempDir.toFile().delete();
            urlClassLoader.close();
        }
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes" })
    void testProcessClassFile_updatesSiblingModuleNameAndClasses() throws Exception {
        setup2();

        File fakeClassFile = mock(File.class);
        when(fakeClassFile.toURI()).thenReturn(Path.of("/tmp/one/com/example/test/FakeClass.class").toUri());

        File fakeDirectory = mock(File.class);
        when(fakeDirectory.toURI()).thenReturn(Path.of("/tmp/one").toUri());
        when(fakeDirectory.toURI().relativize(fakeClassFile.toURI())).thenReturn(Path.of("com/example/test/FakeClass.class").toUri());

        ModuleDescriptor md = mock(ModuleDescriptor.class);

        // Use try-with-resource and mock static method ModuleDescriptor.read
        try (MockedStatic<ModuleDescriptor> mdStatic = Mockito.mockStatic(ModuleDescriptor.class)) {
            mdStatic.when(() -> ModuleDescriptor.read(any(InputStream.class))).thenReturn(md);

            // We call processClassFile with file "module-info.class"
            LinkResolver.siblingModuleName = "";
            LinkResolver.siblingClassNames.clear();

            Class testClass = Class.class;
            URLClassLoader urlClassLoader = mock(URLClassLoader.class);
            when (urlClassLoader.loadClass(any())).thenReturn(testClass);

            LinkResolver.processClassFile(fakeDirectory, fakeClassFile, urlClassLoader);
            assertTrue(LinkResolver.siblingClassNames.contains("java.lang.Class"));
        }
    }

    @Test
    void testRelativizeWithSiblingModule_returnsExpectedPath() {
        setup2();
        // Setup from and to packages and sibling module
        String from = "com.example.sub";
        String to = "com.example.other";
        String toModule = "other.module";

        String relPath = LinkResolver.relativizeWithSiblingModule(from, to, toModule);

        assertNotNull(relPath);
        // Expected path contains toModule directory
        assertTrue(relPath.contains(toModule.replace('.', '/')) || relPath.contains(toModule));
    }

    @Test
    void testResolveSiblingType_resolvesCorrectly() {
        setup2();
        // Prepare static maps to simulate known sibling class and module
        LinkResolver.classToModule.put("com.example.Foo", "example.module");
        LinkResolver.siblingModuleName = "example.module";

        Link ref = new Link();
        ref.setTarget("com.example.Foo");
        ref.setOriginPackage("com.example.other");
        ref.setKind(Link.Kind.UNKNOWN);

        assertTrue(LinkResolver.resolveSiblingType(ref));

        assertTrue(ref.isResolved());
        assertEquals(Link.Scope.SIBLING, ref.getScope());
        assertEquals(Link.Kind.TYPE, ref.getKind());
        assertNotNull(ref.getUri());
        assertTrue(ref.getUri().contains("com/example/Foo".replace('.', '/')) || ref.getUri().contains("com.example.Foo"));
    }

    @Test
    void testResolveSiblingModule_resolvesCorrectly() {
        setup2();
        LinkResolver.siblingModules.add("example.module");

        Link ref = new Link();
        ref.setTarget("example.module");
        ref.setOriginPackage("com.example.other");
        ref.setKind(Link.Kind.UNKNOWN);

        assertTrue(LinkResolver.resolveSiblingModule(ref));

        assertTrue(ref.isResolved());
        assertEquals(Link.Scope.SIBLING, ref.getScope());
        assertEquals(Link.Kind.MODULE, ref.getKind());
        assertNotNull(ref.getUri());
        assertTrue(ref.getUri().contains("example.module"));
    }

    @Test
    void testResolveUnsupported_forUnsupportedPatterns() {
        setup2();
        Link ref = new Link();
        ref.setTarget("?");
        Link result = LinkResolver.resolveUnsupported(ref);
        assertEquals(Link.Kind.UNSUPPORTED, result.getKind());

        Link refWithAngleBrackets = new Link();
        refWithAngleBrackets.setTarget("List<String>");
        Link result2 = LinkResolver.resolveUnsupported(refWithAngleBrackets);
        assertEquals(Link.Kind.UNSUPPORTED, result2.getKind());

        Link normalRef = new Link();
        normalRef.setTarget("com.example.Foo");
        Link result3 = LinkResolver.resolveUnsupported(normalRef);
        assertNotEquals(Link.Kind.UNSUPPORTED, result3.getKind());
    }

    @Test
    void relativizeWithModules_reportsUnknownPackages() {
        reporter.stringWriter = new StringWriter();
        LinkResolver.relativizeWithModules("","unknown.module");
        assertTrue(reporter.stringWriter.toString().contains("Error resolving package"));
    }

    @Test
    void processDirectoryUrl_callsProcessClassFileForClassFiles() throws Exception {
        Path tempDir = Files.createTempDirectory("lr-processdir");
        File dir = tempDir.toFile();
        try {
            // create a dummy class file in the directory
            File classFile = new File(dir, "MyClass.class");
            assertTrue(classFile.createNewFile(), "create dummy class file");

            URL[] urls = new URL[] { dir.toURI().toURL() };

            // Mock processClassFile so we can verify it is invoked
            try (MockedStatic<LinkResolver> lrStatic = Mockito.mockStatic(LinkResolver.class, Mockito.CALLS_REAL_METHODS)) {
                lrStatic.when(() -> LinkResolver.processClassFile(any(File.class), any(File.class), any(URLClassLoader.class)))
                        .thenAnswer(invocation -> {
                            File directoryArg = invocation.getArgument(0);
                            File fileArg = invocation.getArgument(1);
                            // assert the arguments are as expected inside the stub
                            assertEquals(dir.getAbsoluteFile(), directoryArg.getAbsoluteFile());
                            assertEquals(classFile.getName(), fileArg.getName());
                            return null;
                        });

                // Call the real processDirectoryUrl which will call our mocked processClassFile
                LinkResolver.processDirectoryUrl(dir, urls);

                // Verify interaction: at least one call to processClassFile was made
                lrStatic.verify(() -> LinkResolver.processClassFile(eq(dir), eq(classFile), any(URLClassLoader.class)), times(1));
            }
        } finally {
            // cleanup
            Files.deleteIfExists(tempDir.resolve("MyClass.class"));
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void processClassFile_readsModuleInfoAndSetsSiblingModuleName() throws Exception {
        Path tempDir = Files.createTempDirectory("lr-moduleinfo");
        File dir = tempDir.toFile();
        File moduleInfo = new File(dir, "module-info.class");
        try {
            assertTrue(moduleInfo.createNewFile(), "create module-info.class placeholder");

            // Mock ModuleDescriptor.read to avoid parsing actual class bytes
            ModuleDescriptor fakeDescriptor = mock(ModuleDescriptor.class);
            when(fakeDescriptor.name()).thenReturn("fake.module");

            try (MockedStatic<ModuleDescriptor> mdStatic = Mockito.mockStatic(ModuleDescriptor.class)) {
                mdStatic.when(() -> ModuleDescriptor.read(any(InputStream.class))).thenReturn(fakeDescriptor);

                // siblingModuleName should be updated by processClassFile
                LinkResolver.siblingModuleName = "";
                LinkResolver.siblingClassNames.clear();

                // classLoader not needed for module-info branch; pass null
                LinkResolver.processClassFile(dir, moduleInfo, null);

                assertEquals("fake.module", LinkResolver.siblingModuleName, "processClassFile should set siblingModuleName from module-info");
            }
        } finally {
            Files.deleteIfExists(moduleInfo.toPath());
            Files.deleteIfExists(tempDir);
        }
    }

    @Disabled("FILESYSTEM")
    @Test
    void processClassFile_loadsClassAndAddsToSiblingClassNames() throws Exception {
        Path tempDir = Files.createTempDirectory("lr-classfile");
        File dir = tempDir.toFile();
        // create nested path matching package structure: com/example/Test.class
        File packageDir = new File(dir, "com/example");
        assertTrue(packageDir.mkdirs(), "create package directories");
        File testClassFile = new File(packageDir, "Test.class");
        assertTrue(testClassFile.createNewFile(), "create Test.class placeholder");

        try {
            // Provide a mock URLClassLoader that returns a known Class object for the requested class
            URLClassLoader mockLoader = mock(URLClassLoader.class);
            // the className computed by processClassFile will be "com/example/Test" relative path replaced to dots and trimmed of ".class"
            // On most platforms path separators are '/', so relative path "com/example/Test.class" -> "com.example.Test"
            String expectedClassName = "com.example.Test";
            when(mockLoader.loadClass(expectedClassName)).thenAnswer(_ -> (Class<?>)String.class); // use real Class as return

            // Ensure siblingClassNames is empty
            LinkResolver.siblingClassNames.clear();

            LinkResolver.processClassFile(dir, testClassFile, mockLoader);

            // String.class.getName() should be present in siblingClassNames
            assertTrue(LinkResolver.siblingClassNames.contains(String.class.getName()),
                    "siblingClassNames should include the loaded class name");
        } finally {
            // cleanup
            Files.deleteIfExists(testClassFile.toPath());
            Files.deleteIfExists(packageDir.toPath());
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void resolveLocalPackageTypeInternal_null () {
        //"io.github.sandydunlop.markista.model.Node"
        Link link2 = Link.to("unknown.package.Class")
                .fromPackage("io.github.sandydunlop.markista.doclet.MarkdownDoclet.Option");
        boolean r = LinkResolver.resolveLocalPackageOrTypeInternal(link2, "", "Class");
        assertFalse(r);

        r = LinkResolver.resolveLocalPackageOrTypeInternal(link2, "unknown.package", "Class");
        assertFalse(r);
    }

    @Test
    void relativizeWithSiblingModule_returnsPathContainingModuleName() {
        // Basic sanity test: relativizeWithSiblingModule should include provided sibling module component
        String from = "com.example.from";
        String to = "com.example.to";
        String toModule = "some.module";

        String path = LinkResolver.relativizeWithSiblingModule(from, to, toModule);

        assertNotNull(path);
        assertTrue(path.contains(toModule) || path.contains("some" /* fallback check */),
                "Result should mention the sibling module name");
    }

    @Test
    void resolveSiblingType_resolvesWhenClassKnownInMap() {
        Link ref = new Link();
        ref.setOriginPackage("com.example.origin");
        ref.setTarget("com.example.Foo");
        ref.setKind(Link.Kind.UNKNOWN);

        // set up mapping so LinkResolver knows where the class lives
        LinkResolver.classToModule.put("com.example.Foo", "other.module");

        // call method under test
        boolean resolved = LinkResolver.resolveSiblingType(ref);

        assertTrue(resolved);
        assertTrue(ref.isResolved(), "Reference should be marked resolved");
        assertEquals(Link.Scope.SIBLING, ref.getScope(), "Scope should be SIBLING");
        assertEquals(Link.Kind.TYPE, ref.getKind(), "Kind should be TYPE");
        assertNotNull(ref.getUri(), "URI should be set for resolved sibling type");
    }

    @Test
    void resolveSiblingModule_resolvesWhenModuleInSiblingList() {
        Link ref = new Link();
        ref.setOriginPackage("com.example.origin");
        ref.setTarget("sibling.module");
        ref.setKind(Link.Kind.UNKNOWN);

        LinkResolver.siblingModules.add("sibling.module");

        boolean resolved = LinkResolver.resolveSiblingModule(ref);

        assertTrue(resolved);

        assertTrue(ref.isResolved(), "Reference should be marked resolved");
        assertEquals(Link.Scope.SIBLING, ref.getScope(), "Scope should be SIBLING");
        assertEquals(Link.Kind.MODULE, ref.getKind(), "Kind should be MODULE");
        assertNotNull(ref.getUri(), "URI should be set for resolved sibling module");
    }

    @Test
    void resolveUnsupported_marksUnsupportedInputs() {
        Link r1 = new Link();
        r1.setTarget("?");
        Link out1 = LinkResolver.resolveUnsupported(r1);
        assertEquals(Link.Kind.UNSUPPORTED, out1.getKind(), "Single '?' should be treated as unsupported");

        Link r2 = new Link();
        r2.setTarget("List<String>");
        Link out2 = LinkResolver.resolveUnsupported(r2);
        assertEquals(Link.Kind.UNSUPPORTED, out2.getKind(), "Type-like constructs containing '<' should be unsupported");

        Link r3 = new Link();
        r3.setTarget("com.example.Foo");
        Link out3 = LinkResolver.resolveUnsupported(r3);
        // non-unsupported input should not have kind UNSUPPORTED (could be UNKNOWN or other, but not UNSUPPORTED)
        assertNotEquals(Link.Kind.UNSUPPORTED, out3.getKind());
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

    @Test
	void removeParentheses() {
		assertEquals("method", LinkResolver.removeParentheses("method(param1,param2)"));
	}
}
