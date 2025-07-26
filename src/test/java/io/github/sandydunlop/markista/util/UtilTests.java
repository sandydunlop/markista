package io.github.sandydunlop.markista.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UtilTests {
	@Test
	void simplifyName_primitive() {
		assertEquals("int", 
				Utils.simplifyNames("int"));
	}

	@Test
	void simplifyName_simpleName() {
		assertEquals("PackageElement", 
				Utils.simplifyNames("PackageElement"));
	}

	@Test
	void simplifyName_qualifiedName() {
		assertEquals("PackageElement", 
				Utils.simplifyNames("javax.lang.model.element.PackageElement"));
	}

	@Test
	void simplifyName_arrayOfualifiedName() {
		assertEquals("Object[]", 
				Utils.simplifyNames("java.lang.Object[]"));
	}

	@Test
	void simplifyName_setOfQualifiedName() {
		assertEquals("Set<? extends MarkdownDoclet.Option>", Utils.simplifyNames(
					"java.util.Set<? extends io.github.sandydunlop.markdown.MarkdownDoclet.Option>"));
	}
}
