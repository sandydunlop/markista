package io.github.sandydunlop.markista.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LinkResolverTests {
	@Test
	void resolve_qualifiedNativeClass() {
        LinkResolver.addNativeModule("java.base", "https://docs.oracle.com/en/java/javase/24/docs/api/java.base", ".html");
		String link = LinkResolver.resolve("", "java.util.List");
		assertEquals("https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/util/List.html", link);
	}
}
