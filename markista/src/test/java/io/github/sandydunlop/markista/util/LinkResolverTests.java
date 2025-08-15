package io.github.sandydunlop.markista.util;

import com.sun.source.util.DocTreePath;

import io.github.sandydunlop.markista.core.Configuration;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.ClassTypeNode;
import io.github.sandydunlop.markista.model.ModuleNode;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

import jdk.javadoc.doclet.Reporter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
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

import static org.junit.jupiter.api.Assertions.*;
import java.io.InputStream;

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
    private ClassTypeNode node;
    private ClassTypeNode markdownDoclet;

    Reference link;
    private static final String ORIGIN = "com.example";
    private static final String TARGET = "com.example.MyClass";
    private JarFile mockJarFile;
        
	@BeforeEach
    void setup() {
        link = new Reference();
        link.setOrigin(ORIGIN);
        link.setTarget(TARGET);
        link.setKind(Reference.Kind.UNKNOWN);
    }

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
		ctx =  Context.getInstance();
		ctx.setReporter(reporter);
    }

    @BeforeEach
    void init() {
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
		api.addType(new ClassTypeNode("io.github.sandydunlop.markista.util.LinkResolver",
                "LinkResolver", util.getQualifiedName()));
		api.addType(new ClassTypeNode("io.github.sandydunlop.markista.doclet.MarkdownDoclet",
                "MarkdownDoclet", doclet.getQualifiedName()));
		api.addType(new ClassTypeNode("io.github.sandydunlop.markista.doclet.MarkdownDoclet.Option",
                "MarkdownDoclet.Option", doclet.getQualifiedName()));

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

        node = new ClassTypeNode("io.github.sandydunlop.markista.model.Node", 
                "Node", model.getQualifiedName());
        model.addType(node);
        api.addType(node);

        markdownDoclet = new ClassTypeNode("io.github.sandydunlop.markista.doclet.MarkdownDoclet", 
                "MarkdownDoclet", doclet.getQualifiedName());
        model.addType(markdownDoclet);

		LinkResolver.init(api, ctx);
        LinkResolver.addNativeModuleUrl("java.base", "https://docs.oracle.com/en/java/javase/24/docs/api/java.base", ".html");
		LinkResolver.setFlattenedDirectories(null);
        LinkResolver.siblingClassNames = new java.util.HashSet<>();
		ctx.setModuleName("markista");
        ctx.setPackageName("");
	}

	@Test
	void addNativeModuleUrl() {
		LinkResolver.init(api, ctx);
		LinkResolver.setFlattenedDirectories(null);

		String moduleName = "jdk.javadoc";
        LinkResolver.addNativeModuleUrl(moduleName, JAVA_24_URL + moduleName, ".html");

		Reference ref = new Reference();
		ref.setTarget("jdk.javadoc.doclet.Doclet");
		assertTrue(LinkResolver.resolveNativePackageOrType(ref));

		assertEquals(Reference.Scope.NATIVE, ref.getScope());
		assertEquals(Reference.Kind.URL, ref.getKind());
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
		link = LinkResolver.resolve(Reference.to(""));
		assertEquals(Reference.Kind.UNKNOWN, link.getKind());
		assertEquals("", link.getUri());
	}

	@Test
	void resolve_module() {
		link = LinkResolver.resolve(Reference.to("java.base"));
		assertEquals(Reference.Kind.URL, link.getKind());
		assertEquals("https://docs.oracle.com/en/java/javase/24/docs/api/java.base/module-summary.html", link.getUri());
	}

	@Test
	void resolve_packageFromModule() {
		LinkResolver.setFlattenedDirectories("io.github.sandydunlop.markista");
		link = LinkResolver.resolve(Reference.to("io.github.sandydunlop.markista.doclet"));
		assertEquals("doclet", link.getUri());
	}

	@Test
	void resolve_primitive() {
		link = Reference.to("boolean").from("io.github.sandydunlop.markista.util");
		link = LinkResolver.resolve(link);
		assertEquals(Reference.Kind.PRIMITIVE, link.getKind());
	}

	@Test
	void resolve_qualifiedPackage_prevLevel() {
		link = Reference.to("io.github.sandydunlop.markista").from("io.github.sandydunlop.markista.util");
		link = LinkResolver.resolve(link);
		assertEquals("..", link.getUri());
	}

	@Test
	void resolve_qualifiedNativeClass() {
		link = LinkResolver.resolve(Reference.to("java.util.List"));
		link = LinkResolver.resolve(link);
		assertEquals("https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/util/List.html", link.getUri());
	}

	@Test
	void resolve_qualifiedClass_noLevel() {
		link = LinkResolver.resolve(Reference.to("io.github.sandydunlop.markista.model.Node"));
		link = LinkResolver.resolve(link);
		assertEquals("io/github/sandydunlop/markista/model/Node", link.getUri());
	}

	@Test
	void resolve_undefinedType() {
		// This will show a warning in the test output
		link = LinkResolver.resolve(Reference.to("Coso"));
		assertEquals("", link.getUri());
		assertEquals(Reference.Kind.UNKNOWN, link.getKind());
	}

	@Test
	void resolve_unqualifiedPackage_nextLevelPackage() {
		link = Reference.to("doclet").from("io.github.sandydunlop.markista");
		link = LinkResolver.resolve(link);
		assertEquals("doclet", link.getUri());
	}

	@Test
	void resolve_unqualifiedPackage_sameLevelPackage() {
		link = Reference.to("doclet").from("io.github.sandydunlop.markista.util");
		link = LinkResolver.resolve(link);
		assertEquals("../doclet", link.getUri());
	}

	@Test
	void resolve_unqualifiedClass_sameLevel() {
		link = Reference.to("MarkdownDoclet").from("io.github.sandydunlop.markista.util");
		link = LinkResolver.resolve(link);
		assertEquals("../doclet/MarkdownDoclet", link.getUri());
	}

	@Test
	void resolve_unqualifiedPackage_prevLevel() {
		link = Reference.to("markista").from("io.github.sandydunlop.markista.util");
		link = LinkResolver.resolve(link);
		assertEquals("..", link.getUri());
	}

	@Test
	void resolve_void() {
		link = LinkResolver.resolve(Reference.to("void"));
		assertEquals(Reference.Kind.VOID, link.getKind());
		assertEquals("", link.getUri());
	}

	@Test
	void resolveModule() {
		link = Reference.to("markista/");
		link = LinkResolver.resolve(link);
		assertEquals(Reference.Kind.MODULE, link.getKind());
		assertNotEquals("", link.getUri());
		assertNotEquals(null, link.getUri());
	}


    @Test
    void resolve_NullOrigin_SetsOriginFromContext() {
        Reference linkWithoutOrigin = new Reference();
        linkWithoutOrigin.setTarget("java.lang.String");
        linkWithoutOrigin.setKind(Reference.Kind.UNKNOWN);

        Context mockContext = mock(Context.class);
        when(mockContext.getPackageName()).thenReturn("default.pkg");

        Context originalCtx = LinkResolver.ctx;
        LinkResolver.ctx = mockContext;

        Reference resolvedLink = LinkResolver.resolve(linkWithoutOrigin);
        assertEquals("default.pkg", resolvedLink.getOrigin());

        LinkResolver.ctx = originalCtx;
    }

    @Test
    void resolve_EmptyTarget_ReturnsUnchanged() {
        link.setTarget("");
        Reference resolved = LinkResolver.resolve(link);
        // Target empty, should return quickly
        assertEquals("", resolved.getTarget());
        assertFalse(resolved.isResolved());
    }

    @Test
    void resolve_UnsupportedKind_ReturnsImmediately() {
        link.setKind(Reference.Kind.UNSUPPORTED);

        // We simulate resolveUnsupported returns the same link with unsupported kind
        Reference resolved = LinkResolver.resolve(link);
        assertEquals(Reference.Kind.UNSUPPORTED, resolved.getKind());
    }

    @Test
    void resolve_TargetEndsWithSlash_SetsKindModule() {
        module = new ModuleNode("markista");
        api.addModule(module);
        LinkResolver.init(api, ctx);
        link.setTarget("markista/");
        Reference resolved = LinkResolver.resolve(link);
        assertEquals(Reference.Kind.MODULE, resolved.getKind());
    }

    @Test
    void resolve_PrimitiveOrVoid_ResolvesSuccessfully() {
        link.setTarget("void");
        link.setKind(Reference.Kind.UNKNOWN);

        Reference spyLink = spy(link);
        // will call real resolvePrimitiveOrVoid - simulate it resolving the link
        LinkResolver.resolvePrimitiveOrVoid(spyLink);
        Reference resolved = LinkResolver.resolve(spyLink);
        assertTrue(resolved.isResolved());
    }

    @Test
    void tryResolvePrimitiveOrVoid_ReturnsTrueIfResolved() {
        Reference primitiveLink = new Reference();
        primitiveLink.setKind(Reference.Kind.UNKNOWN);
        primitiveLink.setTarget("int");

        boolean result = LinkResolver.resolvePrimitiveOrVoid(primitiveLink);
        assertTrue(result);
    }

    @Test
    void tryResolveNativePackageOrType_ReturnsFalseIfNotResolved() {
        link = new Reference();
        link.setKind(Reference.Kind.TYPE);
        link.setTarget("unknown.Target");

        boolean result = LinkResolver.resolveNativePackageOrType(link);
        // Depending on data, resolveNativePackageOrType may not resolve, result can be false
        assertFalse(result); // just assert the method runs without error
    }

    @Test
    void tryResolveLocalPackageOrType_ReturnsFalseIfNotResolved() {
        link = new Reference();
        link.setKind(Reference.Kind.PACKAGE);
        link.setTarget("local.pkg");

        boolean result = LinkResolver.resolveLocalPackageOrType(link);
        assertFalse(result); // just assert the method runs without error
    }

    @Test
    void tryResolveLocalModule_ReturnsFalseIfNotResolved() {
        link = new Reference();
        link.setKind(Reference.Kind.MODULE);
        link.setTarget("local.module");

        boolean result = LinkResolver.resolveLocalModule(link);
        assertFalse(result); // just assert the method runs without error
    }

    @Test
    void tryResolveSiblingModule_ReturnsFalseIfNotResolved() {
        link = new Reference();
        link.setKind(Reference.Kind.MODULE);
        link.setTarget("sibling.module");

        boolean result = LinkResolver.resolveSiblingModule(link);
        assertFalse(result); // just assert the method runs without error
    }

    @Test
    void tryResolveSiblingType_ReturnsFalseIfNotResolved() {
        link = new Reference();
        link.setKind(Reference.Kind.TYPE);
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

        Reference ref = new Reference();
        ref.setTarget("com.example.Foo");
        ref.setOrigin("com.example.other");
        ref.setKind(Reference.Kind.UNKNOWN);

        assertTrue(LinkResolver.resolveSiblingType(ref));

        assertTrue(ref.isResolved());
        assertEquals(Reference.Scope.SIBLING, ref.getScope());
        assertEquals(Reference.Kind.TYPE, ref.getKind());
        assertNotNull(ref.getUri());
        assertTrue(ref.getUri().contains("com/example/Foo".replace('.', '/')) || ref.getUri().contains("com.example.Foo"));
    }

    @Test
    void testResolveSiblingModule_resolvesCorrectly() {
        setup2();
        LinkResolver.siblingModules.add("example.module");

        Reference ref = new Reference();
        ref.setTarget("example.module");
        ref.setOrigin("com.example.other");
        ref.setKind(Reference.Kind.UNKNOWN);

        assertTrue(LinkResolver.resolveSiblingModule(ref));

        assertTrue(ref.isResolved());
        assertEquals(Reference.Scope.SIBLING, ref.getScope());
        assertEquals(Reference.Kind.MODULE, ref.getKind());
        assertNotNull(ref.getUri());
        assertTrue(ref.getUri().contains("example.module"));
    }

    @Test
    void testResolveUnsupported_forUnsupportedPatterns() {
        setup2();
        Reference ref = new Reference();
        ref.setTarget("?");
        Reference result = LinkResolver.resolveUnsupported(ref);
        assertEquals(Reference.Kind.UNSUPPORTED, result.getKind());

        Reference refWithAngleBrackets = new Reference();
        refWithAngleBrackets.setTarget("List<String>");
        Reference result2 = LinkResolver.resolveUnsupported(refWithAngleBrackets);
        assertEquals(Reference.Kind.UNSUPPORTED, result2.getKind());

        Reference normalRef = new Reference();
        normalRef.setTarget("com.example.Foo");
        Reference result3 = LinkResolver.resolveUnsupported(normalRef);
        assertNotEquals(Reference.Kind.UNSUPPORTED, result3.getKind());
    }
}
