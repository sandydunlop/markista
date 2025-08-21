package io.github.sandydunlop.markista.assembler;

import io.github.sandydunlop.markista.common.Utils;
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
	void removeParentheses() {
		assertEquals("method", Utils.removeParentheses("method(param1,param2)"));
	}

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
	void simplifyName_arrayOfQualifiedName() {
		assertEquals("Object[]", 
				Utils.simplifyNames("java.lang.Object[]"));
	}

	@Test
	void simplifyName_setOfQualifiedName() {
		assertEquals("Set<? extends MarkdownDoclet.Option>", Utils.simplifyNames(
					"java.util.Set<? extends io.github.sandydunlop.markdown.MarkdownDoclet.Option>"));
	}
}
