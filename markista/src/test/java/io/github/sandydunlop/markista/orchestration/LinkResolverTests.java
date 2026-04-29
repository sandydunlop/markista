package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.ModelTestEnvironment;
import io.github.sandydunlop.markista.core.Configuration;
import io.github.qishr.cascara.lang.java.model.ClassNode;
import io.github.qishr.cascara.lang.java.model.ModuleNode;
import io.github.qishr.cascara.lang.java.model.NameUtil;
import io.github.qishr.cascara.lang.java.model.JlsName;
import io.github.qishr.cascara.lang.java.model.Link;
import io.github.qishr.cascara.lang.java.model.MethodNode;
import io.github.qishr.cascara.lang.java.model.Reference;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class LinkResolverTests extends ModelTestEnvironment {
    private ClassNode markdownDoclet;

    @BeforeEach
    void init() {
        setupModel();
        markdownDoclet = newClass("MarkdownDoclet", doclet);
        model.addType(markdownDoclet);

        Configuration.setFlattenPackages(true);
		ctx.setModuleName("markista");
        ctx.setPackageName("");
        ctx.setApi(api);
        resolver = new LinkResolver(api, ctx);
    }

	@Test
	void resolve_module() {
		Link link = Link.to(NameUtil.createReference("java.base/"));
        resolver.resolveLink(link);
		assertEquals(Link.Kind.MODULE, link.getKind());
		assertEquals("https://docs.oracle.com/en/java/javase/24/docs/api/java.base/module-summary.html", link.getUri().toString());
	}

	@Test
	void resolve_packageFromModule() {
		Link link = Link.to(NameUtil.createReference("io.github.sandydunlop.markista.doclet"));
        resolver.resolveLink(link);
		assertEquals("sandydunlop/markista/doclet/", link.getUri().toString());
	}

	@Test
	void resolve_primitive() {
        ctx.setPackageName("io.github.sandydunlop.markista.util");
		Link link = Link.to(NameUtil.createReference("boolean"));
		resolver.resolveLink(link);
		assertEquals(Link.Kind.PRIMITIVE, link.getKind());
	}

	@Test
	void resolve_qualifiedPackage_prevLevel() {
        ctx.setPackageName("io.github.sandydunlop.markista.model");
		Link link = Link.to(NameUtil.createReference("io.github.sandydunlop.markista"));
		resolver.resolveLink(link);
		assertEquals("../", link.getUri().toString());
	}

	@Test
	void resolve_qualifiedStandardClass() {
		Link link = Link.to(NameUtil.createReference("java.util.List"));
        resolver.resolveLink(link);
		assertEquals("https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/util/List.html", link.getUri().toString());
	}

	@Test
	void resolve_qualifiedClass_noLevel() {
		Link link = Link.to(NameUtil.createReference("io.github.sandydunlop.markista.model.Node"));
		resolver.resolveLink(link);
		assertEquals("sandydunlop/markista/model/Node", link.getUri().toString());
	}

	@Test
	void resolve_qualifiedClass_otherLevel() {
        ctx.setPackageName("io.github.sandydunlop.markista.doclet");
		Link link = Link.to(NameUtil.createReference("io.github.sandydunlop.markista.model.Node"));
		resolver.resolveLink(link);
		assertEquals("../model/Node", link.getUri().toString());
	}

	@Test
	void resolve_undefinedType() {
		// This will show a warning in the test output
		Link link = Link.to(NameUtil.createReference("Coso"));
        resolver.resolveLink(link);
		assertNull(link.getUri());
		assertEquals(Link.Kind.UNRESOLVED, link.getKind());
	}

	@Test
	void resolve_standard_package() {
		Link link = Link.to(NameUtil.createReference("java.lang"));
		resolver.resolveLink(link);
		assertEquals("https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/lang.html", link.getUri().toString());
	}

    @Disabled("New LinkResolver")
	@Test
	void resolve_unqualifiedPackage_nextLevelPackage() {
        ctx.setPackageName("io.github.sandydunlop.markista");
		Link link = Link.to(NameUtil.createReference("doclet"));
		resolver.resolveLink(link);
		assertEquals("doclet", link.getUri().toString());
	}

    @Disabled("New LinkResolver")
	@Test
	void resolve_unqualifiedPackage_sameLevelPackage() {
        ctx.setPackageName("io.github.sandydunlop.markista");
		Link link = Link.to(NameUtil.createReference("doclet"));
		resolver.resolveLink(link);
		assertEquals("../doclet", link.getUri().toString());
	}

	@Test
	void resolve_unqualifiedClass_sameLevel() {
        ctx.setPackageName("io.github.sandydunlop.markista.model");
		Link link = Link.to(NameUtil.createReference("MarkdownDoclet"));
		resolver.resolveLink(link);
		assertEquals("../doclet/MarkdownDoclet", link.getUri().toString());
	}

	@Test
	void resolve_unqualifiedClass_otherLevel() {
        ctx.setPackageName("io.github.sandydunlop.markista");
		Link link = Link.to(NameUtil.createReference("MarkdownDoclet"));
		resolver.resolveLink(link);
		assertEquals("doclet/MarkdownDoclet", link.getUri().toString());
	}

    @Disabled("New LinkResolver")
	@Test
	void resolve_unqualifiedPackage_prevLevel() {
        ctx.setPackageName("io.github.sandydunlop.markista.model");
		Link link = Link.to(NameUtil.createReference("markista"));
		resolver.resolveLink(link);
		assertEquals("..", link.getUri().toString());
	}

	@Test
	void resolve_void() {
		Link link = Link.to(NameUtil.createReference("void"));
        resolver.resolveLink(link);
		assertEquals(Link.Kind.VOID, link.getKind());
		assertNull(link.getUri());
	}

    @Disabled
	@Test
	void resolveModule() {
		Link link = Link.to(NameUtil.createReference("markista/"));
		resolver.resolveLink(link);
		assertEquals(Link.Kind.MODULE, link.getKind());
		assertNotEquals(null, link.getUri());
		assertNotEquals("", link.getUri().toString());
	}


    @Disabled
    @Test
    void resolve_TargetEndsWithSlash_SetsKindModule() {
        module = new ModuleNode("markista");
        api.addModule(module);
        resolver = new LinkResolver(api, ctx);
        Link link = Link.to(NameUtil.createReference("markista/"));
        resolver.resolveLink(link);
        assertEquals(Link.Kind.MODULE, link.getKind());
    }

    @Test
    void resolve_PrimitiveOrVoid_ResolvesSuccessfully() {
        Link link = Link.to(NameUtil.createReference(("void")));
        resolver.resolveLink(link);
        assertTrue(link.isResolved());
    }

    @Test
    void tryResolveStandardPackageOrType_ReturnsFalseIfNotResolved() {
        Link link = Link.to(NameUtil.createReference("unknown.Target"));
        link.setKind(Link.Kind.TYPE);

        boolean result = resolver.resolveLink(link);
        // Depending on data, resolveStandardPackageOrType may not resolve, result can be false
        assertFalse(result); // just assert the method runs without error
    }

    @Test
    void tryResolveLocalPackageOrType_ReturnsFalseIfNotResolved() {
        Link link = Link.to(NameUtil.createReference("local.pkg"));
        link.setKind(Link.Kind.PACKAGE);

        boolean result = resolver.resolveLink(link);
        assertFalse(result); // just assert the method runs without error
    }

    @Test
    void tryResolveLocalModule_ReturnsFalseIfNotResolved() {
        Link link = Link.to(NameUtil.createReference("local.module"));
        link.setKind(Link.Kind.MODULE);

        boolean result = resolver.resolveLink(link);
        assertFalse(result); // just assert the method runs without error
    }

    @Test
    void tryResolveSiblingModule_ReturnsFalseIfNotResolved() {
        Link link = Link.to(NameUtil.createReference("sibling.module"));
        link.setKind(Link.Kind.MODULE);

        boolean result = resolver.resolveLink(link);
        assertFalse(result); // just assert the method runs without error
    }

    @Test
    void tryResolveSiblingType_ReturnsFalseIfNotResolved() {
        Link link = Link.to(NameUtil.createReference("sibling.type"));
        link.setKind(Link.Kind.TYPE);

        boolean result = resolver.resolveLink(link);
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

    // @Test
    // void testRelativizeWithSiblingModule_returnsExpectedPath() {
    //     setup2();
    //     // Setup from and to packages and sibling module
    //     String from = "com.example.sub";
    //     String to = "com.example.other";
    //     String toModule = "other.module";

    //     String relPath = LinkResolver.relativizeWithSiblingModule(from, to, toModule);

    //     assertNotNull(relPath);
    //     // Expected path contains toModule directory
    //     assertTrue(relPath.contains(toModule.replace('.', '/')) || relPath.contains(toModule));
    // }

    @Test
    void resolveLocalPackageTypeInternal_null () {
        ctx.setPackageName("io.github.sandydunlop.markista.doclet.MarkdownDoclet.Option");
        Link link2 = Link.to(NameUtil.createReference("unknown.package.Class"));
        boolean r = resolver.resolveLink(link2);
        assertFalse(r);

        r = resolver.resolveLink(link2);
        assertFalse(r);
    }

    // @Test
    // void relativizeWithSiblingModule_returnsPathContainingModuleName() {
    //     // Basic sanity test: relativizeWithSiblingModule should include provided sibling module component
    //     String from = "com.example.from";
    //     String to = "com.example.to";
    //     String toModule = "some.module";

    //     String path = LinkResolver.relativizeWithSiblingModule(from, to, toModule);

    //     assertNotNull(path);
    //     assertTrue(path.contains(toModule) || path.contains("some" /* fallback check */),
    //             "Result should mention the sibling module name");
    // }

    @Test
	void removeParentheses() {
		assertEquals("method", resolver.removeParentheses("method(param1,param2)"));
	}

    @Test
    void test_newResolver() {
        // Add a 'Doclet' class. 'Node' class is already there.
        ClassNode docletType = newClass("Doclet", doclet);

        JlsName originName = docletType.getName(); //ModelUtil.createName(docletType.getName().fullyQualifiedName(), docletType.getPackageName());
        JlsName targetName = node.getName(); //ModelUtil.createName(node.getName().fullyQualifiedName(), docletType.getPackageName());

        // Both within the "markista" module
        Reference origin = NameUtil.createReference("markista", originName);
        Reference target = NameUtil.createReference("markista", targetName);

        Link link = Link.to(target).from(origin);

        LinkResolver resolver = new LinkResolver(api, ctx);

        resolver.resolveLink(link);
        assertTrue(link.isResolved());
        assertEquals("../model/Node", link.getUri().toString());

        String str = link.toString();
        assertEquals("type:type:markista/io.github.sandydunlop.markista.model.Node", str);
    }

    @Test
    void test_qualify_1() {
        Reference ref = NameUtil.createReference("io.github.sandydunlop.markista");
        resolver.qualify(ref);
        JlsName name = ref.getName();
        assertTrue(name.isPackage());
    }

    @Disabled
    @Test
    void test_qualify_2() {
        Reference ref = NameUtil.createReference("io.github.qishr.cascara.lang.java.model.Node");
        resolver.qualify(ref);
        JlsName name = ref.getName();
        assertTrue(name.isType());
    }

    @Test
    void test_jreType() {
        Reference ref = NameUtil.createReference("jdk.javadoc.doclet.Doclet.Option#process(java.lang.String,java.util.List)");
        resolver.qualify(ref);
        JlsName name = ref.getName();
        assertTrue(name.isMember());
        assertEquals("process", name.memberName().toString());
    }

    @Test
    void test_url() {
        Link link = Link.toWeb(URI.create("https://example.com"));
        resolver.resolveLink(link);
        assertTrue(link.isResolved());
    }

    @Disabled
    @Test
    void test_method() {
        JlsName methodName = NameUtil.createMemberName("testMethod");
        MethodNode test = new MethodNode("java.lang.String", methodName);
        test.setOwnerName(node.getName());
        node.addMethod(test);
        api.addMethod(test);
        Reference reference = NameUtil.createReference("", methodName);
        Link link = Link.to(reference);
        resolver.resolveLink(link);
        assertTrue(link.isResolved());
    }
}
