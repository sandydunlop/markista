package io.github.sandydunlop.markista.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
		PackageNode markista = new PackageNode("io.github.sandydunlop.markista");
		PackageNode util = new PackageNode("io.github.sandydunlop.markista.util");
		PackageNode doclet = new PackageNode("io.github.sandydunlop.markista.doclet");
		api.addPackage(markista);
		api.addPackage(util);
		api.addPackage(doclet);
		api.addClass(new ClassNode("io.github.sandydunlop.markista.util.LinkResolver","LinkResolver", util));
		api.addClass(new ClassNode("io.github.sandydunlop.markista.doclet.MarkdownDoclet","MarkdownDoclet", doclet));
		api.addClass(new ClassNode("io.github.sandydunlop.markista.doclet.MarkdownDoclet.Option","MarkdownDoclet.Option", doclet));
		LinkResolver.setApi(api);
        LinkResolver.addNativeModule("java.base", "https://docs.oracle.com/en/java/javase/24/docs/api/java.base", ".html");
    }

    @BeforeEach
    void init() {
		LinkResolver.setSquashedDirectories(null);
    }

	@Test
	void resolve_qualifiedNativeClass() {
		Reference link = LinkResolver.resolve("java.util.List");
		assertEquals("https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/util/List.html", link.getUri());
	}

	@Test
	void resolve_unqualifiedPackage_nextLevelPackage() {
		Reference link = LinkResolver.resolve("io.github.sandydunlop.markista", "doclet");
		assertEquals("doclet", link.getUri());
	}

	@Test
	void resolve_unqualifiedPackage_sameLevelPackage() {
		Reference link = LinkResolver.resolve("io.github.sandydunlop.markista.util", "doclet");
		assertEquals("../doclet", link.getUri());
	}

	@Test
	void resolve_unqualifiedlClass_sameLevel() {
		Reference link = LinkResolver.resolve("io.github.sandydunlop.markista.util", "MarkdownDoclet");
		assertEquals("../doclet/MarkdownDoclet", link.getUri());
	}

	@Test
	void resolve_unqualifiedlPackage_prevLevel() {
		Reference link = LinkResolver.resolve("io.github.sandydunlop.markista.util", "markista");
		assertEquals("..", link.getUri());
	}

	@Test
	void resolve_qualifiedlPackage_prevLevel() {
		Reference link = LinkResolver.resolve("io.github.sandydunlop.markista.util", "io.github.sandydunlop.markista");
		assertEquals("..", link.getUri());
	}

	@Test
	void resolve_primitive() {
		Reference link = LinkResolver.resolve("io.github.sandydunlop.markista.util", "boolean");
		assertEquals(Reference.Kind.PRIMITIVE, link.getKind());
	}

	@Test
	void relativize_toParentLevel() {
		String link = LinkResolver.relativize("io.github.sandydunlop.markista.util", "io.github.sandydunlop.markista");
		assertEquals("..", link);
	}

	@Test
	void relativize_toSameLevel() {
		String link = LinkResolver.relativize("io.github.sandydunlop.markista.util", "io.github.sandydunlop.markista.doclet");
		assertEquals("../doclet", link);
	}

	@Test
	void relativize_squashed_toSameLevel() {
		LinkResolver.setSquashedDirectories("io.github.sandydunlop.markista");
		String link = LinkResolver.relativize("io.github.sandydunlop.markista.util", "io.github.sandydunlop.markista.doclet");
		assertEquals("../doclet", link);
	}
}
