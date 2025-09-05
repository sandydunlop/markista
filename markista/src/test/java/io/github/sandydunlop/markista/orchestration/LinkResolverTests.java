package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.core.Configuration;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.Link;

import java.io.StringWriter;

import javax.lang.model.element.Element;

import jdk.javadoc.doclet.Reporter;

import com.sun.source.util.DocTreePath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class LinkResolverTests {
    LinkResolver resolver;
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

    TestReporter reporter;

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

		resolver = new LinkResolver(api, ctx);
		Relativizer.setFlattenedDirectories(null);
		ctx.setModuleName("markista");
        ctx.setPackageName("");

        link = new Link();
        link.setOriginPackage(ORIGIN);
        link.setTarget(TARGET);
        link.setKind(Link.Kind.UNKNOWN);
    }

	@Test
	void resolve_empty() {
		link = Link.to("");
        resolver.resolve(link);
		assertEquals(Link.Kind.UNKNOWN, link.getKind());
		assertEquals("", link.getPath());
	}

	@Test
	void resolve_module() {
		link = Link.to("java.base");
        resolver.resolve(link);
		assertEquals(Link.Kind.URL, link.getKind());
		assertEquals("https://docs.oracle.com/en/java/javase/24/docs/api/java.base/module-summary.html", link.getPath());
	}

	@Test
	void resolve_packageFromModule() {
		Relativizer.setFlattenedDirectories("io.github.sandydunlop.markista");
		link = Link.to("io.github.sandydunlop.markista.doclet");
        resolver.resolve(link);
		assertEquals("doclet", link.getPath());
	}

	@Test
	void resolve_primitive() {
		link = Link.to("boolean").fromPackage("io.github.sandydunlop.markista.util");
		resolver.resolve(link);
		assertEquals(Link.Kind.PRIMITIVE, link.getKind());
	}

	@Test
	void resolve_qualifiedPackage_prevLevel() {
		link = Link.to("io.github.sandydunlop.markista").fromPackage("io.github.sandydunlop.markista.util");
		resolver.resolve(link);
		assertEquals("..", link.getPath());
	}

	@Test
	void resolve_qualifiedStandardClass() {
		link = Link.to("java.util.List");
        resolver.resolve(link);
		assertEquals("https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/util/List.html", link.getPath());
	}

	@Test
	void resolve_qualifiedClass_noLevel() {
		link = Link.to("io.github.sandydunlop.markista.model.Node");
		resolver.resolve(link);
		assertEquals("io/github/sandydunlop/markista/model/Node", link.getPath());
	}

	@Test
	void resolve_qualifiedClass_otherLevel() {
        ctx.setPackageName("io.github.sandydunlop.markista.doclet");
		link = Link.to("io.github.sandydunlop.markista.model.Node");
		resolver.resolve(link);
		assertEquals("../model/Node", link.getPath());
	}

	@Test
	void resolve_undefinedType() {
		// This will show a warning in the test output
		link = Link.to("Coso");
        resolver.resolve(link);
		assertEquals("", link.getPath());
		assertEquals(Link.Kind.UNKNOWN, link.getKind());
	}

	@Test
	void resolve_standard_package() {
		link = Link.to("java.lang");
		resolver.resolve(link);
		assertEquals("https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/lang.html", link.getPath());
	}

    @Disabled("New LinkResolver")
	@Test
	void resolve_unqualifiedPackage_nextLevelPackage() {
		link = Link.to("doclet").fromPackage("io.github.sandydunlop.markista");
		resolver.resolve(link);
		assertEquals("doclet", link.getPath());
	}

    @Disabled("New LinkResolver")
	@Test
	void resolve_unqualifiedPackage_sameLevelPackage() {
		link = Link.to("doclet").fromPackage("io.github.sandydunlop.markista.util");
		resolver.resolve(link);
		assertEquals("../doclet", link.getPath());
	}

	@Test
	void resolve_unqualifiedClass_sameLevel() {
		link = Link.to("MarkdownDoclet").fromPackage("io.github.sandydunlop.markista.util");
		resolver.resolve(link);
		assertEquals("../doclet/MarkdownDoclet", link.getPath());
	}

    @Disabled("New LinkResolver")
	@Test
	void resolve_unqualifiedPackage_prevLevel() {
		link = Link.to("markista").fromPackage("io.github.sandydunlop.markista.util");
		resolver.resolve(link);
		assertEquals("..", link.getPath());
	}

	@Test
	void resolve_void() {
		link = Link.to("void");
        resolver.resolve(link);
		assertEquals(Link.Kind.VOID, link.getKind());
		assertEquals("", link.getPath());
	}

	@Test
	void resolveModule() {
		link = Link.to("markista/");
		resolver.resolve(link);
		assertEquals(Link.Kind.MODULE, link.getKind());
		assertNotEquals("", link.getPath());
		assertNotEquals(null, link.getPath());
	}

    @Test
    void resolve_EmptyTarget_ReturnsUnchanged() {
        link.setTarget("");
        resolver.resolve(link);
        // Target empty, should return quickly
        assertEquals("", link.getTarget());
        assertFalse(link.isResolved());
    }

    @Test
    void resolve_UnsupportedKind_ReturnsImmediately() {
        link.setKind(Link.Kind.UNSUPPORTED);

        // We simulate resolveUnsupported returns the same link with unsupported kind
        resolver.resolve(link);
        assertEquals(Link.Kind.UNSUPPORTED, link.getKind());
    }

    @Test
    void resolve_TargetEndsWithSlash_SetsKindModule() {
        module = new ModuleNode("markista");
        api.addModule(module);
        resolver = new LinkResolver(api, ctx);
        link.setTarget("markista/");
        resolver.resolve(link);
        assertEquals(Link.Kind.MODULE, link.getKind());
    }

    @Test
    void resolve_PrimitiveOrVoid_ResolvesSuccessfully() {
        link.setTarget("void");
        link.setKind(Link.Kind.UNKNOWN);
        resolver.resolve(link);
        assertTrue(link.isResolved());
    }

    @Test
    void tryResolvePrimitiveOrVoid_ReturnsTrueIfResolved() {
        Link primitiveLink = new Link();
        primitiveLink.setKind(Link.Kind.UNKNOWN);
        primitiveLink.setTarget("int");

        boolean result = resolver.resolvePrimitiveOrVoid(primitiveLink);
        assertTrue(result);
    }

    @Test
    void tryResolveStandardPackageOrType_ReturnsFalseIfNotResolved() {
        link = new Link();
        link.setKind(Link.Kind.TYPE);
        link.setTarget("unknown.Target");

        boolean result = resolver.resolve(link);
        // Depending on data, resolveStandardPackageOrType may not resolve, result can be false
        assertFalse(result); // just assert the method runs without error
    }

    @Test
    void tryResolveLocalPackageOrType_ReturnsFalseIfNotResolved() {
        link = new Link();
        link.setKind(Link.Kind.PACKAGE);
        link.setTarget("local.pkg");

        boolean result = resolver.resolveLocalPackageOrType(link);
        assertFalse(result); // just assert the method runs without error
    }

    @Test
    void tryResolveLocalModule_ReturnsFalseIfNotResolved() {
        link = new Link();
        link.setKind(Link.Kind.MODULE);
        link.setTarget("local.module");

        boolean result = resolver.resolveLocalModule(link);
        assertFalse(result); // just assert the method runs without error
    }

    @Test
    void tryResolveSiblingModule_ReturnsFalseIfNotResolved() {
        link = new Link();
        link.setKind(Link.Kind.MODULE);
        link.setTarget("sibling.module");

        boolean result = resolver.resolve(link);
        assertFalse(result); // just assert the method runs without error
    }

    @Test
    void tryResolveSiblingType_ReturnsFalseIfNotResolved() {
        link = new Link();
        link.setKind(Link.Kind.TYPE);
        link.setTarget("sibling.type");

        boolean result = resolver.resolve(link);
        assertFalse(result);
    }

    @BeforeEach
    void setUp() {
        // Assuming Configuration is a static class with a static method getListExternal
        Configuration.setAddModules("");
	}



    void setup2() {
        Configuration.setModulePaths("");
        Configuration.setAddModules("");
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
    void testResolveUnsupported_forUnsupportedPatterns() {
        setup2();
        Link ref = new Link();
        ref.setTarget("?");
        Link result = resolver.resolveUnsupported(ref);
        assertEquals(Link.Kind.UNSUPPORTED, result.getKind());

        Link refWithAngleBrackets = new Link();
        refWithAngleBrackets.setTarget("List<String>");
        Link result2 = resolver.resolveUnsupported(refWithAngleBrackets);
        assertEquals(Link.Kind.UNSUPPORTED, result2.getKind());

        Link normalRef = new Link();
        normalRef.setTarget("com.example.Foo");
        Link result3 = resolver.resolveUnsupported(normalRef);
        assertNotEquals(Link.Kind.UNSUPPORTED, result3.getKind());
    }

    @Test
    void relativizeWithModules_reportsUnknownPackages() {
        reporter.stringWriter = new StringWriter();
        resolver.relativizeWithModules("","unknown.module");
        assertTrue(reporter.stringWriter.toString().contains("Error resolving package"));
    }

    @Test
    void resolveLocalPackageTypeInternal_null () {
        //"io.github.sandydunlop.markista.model.Node"
        Link link2 = Link.to("unknown.package.Class")
                .fromPackage("io.github.sandydunlop.markista.doclet.MarkdownDoclet.Option");
        boolean r = resolver.resolve(link2);
        assertFalse(r);

        r = resolver.resolve(link2);
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
    void resolveUnsupported_marksUnsupportedInputs() {
        Link r1 = new Link();
        r1.setTarget("?");
        Link out1 = resolver.resolveUnsupported(r1);
        assertEquals(Link.Kind.UNSUPPORTED, out1.getKind(), "Single '?' should be treated as unsupported");

        Link r2 = new Link();
        r2.setTarget("List<String>");
        Link out2 = resolver.resolveUnsupported(r2);
        assertEquals(Link.Kind.UNSUPPORTED, out2.getKind(), "Type-like constructs containing '<' should be unsupported");

        Link r3 = new Link();
        r3.setTarget("com.example.Foo");
        Link out3 = resolver.resolveUnsupported(r3);
        // non-unsupported input should not have kind UNSUPPORTED (could be UNKNOWN or other, but not UNSUPPORTED)
        assertNotEquals(Link.Kind.UNSUPPORTED, out3.getKind());
    }

    @Test
	void removeParentheses() {
		assertEquals("method", resolver.removeParentheses("method(param1,param2)"));
	}
}
