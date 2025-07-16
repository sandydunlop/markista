package io.github.sandydunlop.markista.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NameUtilsTests {
	@Test
	void simplifyName_primitive() {
		assertEquals("int", 
				NameUtils.simplifyNames("int"));
	}

	@Test
	void simplifyName_simpleName() {
		assertEquals("PackageElement", 
				NameUtils.simplifyNames("PackageElement"));
	}

	@Test
	void simplifyName_qualifiedName() {
		assertEquals("PackageElement", 
				NameUtils.simplifyNames("javax.lang.model.element.PackageElement"));
	}

	@Test
	void simplifyName_arrayOfualifiedName() {
		assertEquals("Object[]", 
				NameUtils.simplifyNames("java.lang.Object[]"));
	}

	@Test
	void simplifyName_setOfQualifiedName() {
		assertEquals("Set<? extends MarkdownDoclet.Option>", NameUtils.simplifyNames(
					"java.util.Set<? extends io.github.sandydunlop.markdown.MarkdownDoclet.Option>"));
	}
}
