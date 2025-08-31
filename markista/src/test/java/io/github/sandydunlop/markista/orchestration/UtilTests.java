package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.markdown.MarkdownUtils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilTests {
	@Test
	void inOneLine() {
		assertEquals("one two three", MarkdownUtils.inOneLine("one\ntwo\nthree"));
	}

	@Test
	void inOneLine_emptyString() {
		assertEquals("", MarkdownUtils.inOneLine(""));
	}

	@Test
	void inOneLine_null() {
		assertEquals("", MarkdownUtils.inOneLine(null));
	}

	@Test
	void simplifyName_primitive() {
		assertEquals("int",
				Context.NameSimplifier.simplifyNames("int"));
	}

	@Test
	void simplifyName_simpleName() {
		assertEquals("PackageElement",
				Context.NameSimplifier.simplifyNames("PackageElement"));
	}

	@Test
	void simplifyName_qualifiedName() {
		assertEquals("PackageElement",
				Context.NameSimplifier.simplifyNames("javax.lang.model.element.PackageElement"));
	}

	@Test
	void simplifyName_arrayOfQualifiedName() {
		assertEquals("Object[]",
				Context.NameSimplifier.simplifyNames("java.lang.Object[]"));
	}

	@Test
	void simplifyName_setOfQualifiedName() {
		assertEquals("Set<? extends MarkdownDoclet.Option>", Context.NameSimplifier.simplifyNames(
					"java.util.Set<? extends io.github.sandydunlop.markdown.MarkdownDoclet.Option>"));
	}
}
