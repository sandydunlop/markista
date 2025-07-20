package io.github.sandydunlop.markista.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UtilTests {
	@Test
	void simplifyName_primitive() {
		assertEquals("int", 
				Util.simplifyNames("int"));
	}

	@Test
	void simplifyName_simpleName() {
		assertEquals("PackageElement", 
				Util.simplifyNames("PackageElement"));
	}

	@Test
	void simplifyName_qualifiedName() {
		assertEquals("PackageElement", 
				Util.simplifyNames("javax.lang.model.element.PackageElement"));
	}

	@Test
	void simplifyName_arrayOfualifiedName() {
		assertEquals("Object[]", 
				Util.simplifyNames("java.lang.Object[]"));
	}

	@Test
	void simplifyName_setOfQualifiedName() {
		assertEquals("Set<? extends MarkdownDoclet.Option>", Util.simplifyNames(
					"java.util.Set<? extends io.github.sandydunlop.markdown.MarkdownDoclet.Option>"));
	}
}
