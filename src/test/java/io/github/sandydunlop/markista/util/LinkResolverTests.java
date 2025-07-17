package io.github.sandydunlop.markista.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.ClassNode;

class LinkResolverTests {
	private static Api api;
    @BeforeAll
    static void initAll() {
		api = new Api();
		api.addPackage(new PackageNode("io.github.sandydunlop"));
		api.addPackage(new PackageNode("io.github.sandydunlop.markista"));
		api.addPackage(new PackageNode("io.github.sandydunlop.markista.util"));
		api.addPackage(new PackageNode("io.github.sandydunlop.markista.doclet"));
		api.addClass(new ClassNode("io.github.sandydunlop.markista.util.LinkResolver","LinkResolver","io.github.sandydunlop.markista.util"));
		api.addClass(new ClassNode("io.github.sandydunlop.markista.doclet.MarkdownDoclet","MarkdownDoclet","io.github.sandydunlop.markista.doclet"));
		api.addClass(new ClassNode("io.github.sandydunlop.markista.doclet.MarkdownDoclet.Option","MarkdownDoclet.Option","io.github.sandydunlop.markista.doclet"));
		LinkResolver.setApi(api);
        LinkResolver.addNativeModule("java.base", "https://docs.oracle.com/en/java/javase/24/docs/api/java.base", ".html");
    }

	@Test
	void resolve_qualifiedNativeClass() {
		Reference link = LinkResolver.resolve("", "java.util.List");
		assertEquals("https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/util/List.html", link.url);
	}

	@Test
	void resolve_unqualifiedPackage_nextLevelPackage() {
		Reference link = LinkResolver.resolve("io.github.sandydunlop.markista", "doclet");
		assertEquals("doclet", link.path);
	}

	@Test
	void resolve_unqualifiedPackage_sameLevelPackage() {
		Reference link = LinkResolver.resolve("io.github.sandydunlop.markista.util", "doclet");
		assertEquals("../doclet", link.path);
	}

	@Test
	void resolve_unqualifiedlClass_sameLevel() {
		Reference link = LinkResolver.resolve("io.github.sandydunlop.markista.util", "MarkdownDoclet");
		assertEquals("../doclet/MarkdownDoclet", link.path);
	}

	@Test
	void resolve_unqualifiedlPackage_prevLevel() {
		Reference link = LinkResolver.resolve("io.github.sandydunlop.markista.util", "markista");
		assertEquals("..", link.path);
	}

	@Test
	void resolve_qualifiedlPackage_prevLevel() {
		Reference link = LinkResolver.resolve("io.github.sandydunlop.markista.util", "io.github.sandydunlop.markista");
		assertEquals("..", link.path);
	}

	@Test
	void relativize_1() {
		String link = LinkResolver.relativize("io.github.sandydunlop.markista.util", "io.github.sandydunlop.markista");
		assertEquals("..", link);
	}

	// @Test
	// void relativize_primitive() {
	// 	Reference link = LinkResolver.relativize("io.github.sandydunlop.markista.util", "boolean");
	// 	assertEquals(null, link.path);
	// }

	@Test
	void resolve_primitive() {
		Reference link = LinkResolver.resolve("io.github.sandydunlop.markista.util", "boolean");
		assertEquals(Reference.Kind.NONE, link.kind);
	}
}
