package io.github.sandydunlop.markista.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.sandydunlop.markista.model.Text;

class UtilTests {
	@Test
	void inOneLine() {
		assertEquals("one two three", Utils.inOneLine("one\ntwo\nthree"));
	}

	@Test
	void inOneLine_emptyString() {
		assertEquals("", Utils.inOneLine(""));
	}

	@Test
	void inOneLine_null() {
		assertEquals("", Utils.inOneLine(null));
	}

	@Test
	void isNullOrEmptyString() {
		String nullString = null;
		assertEquals(true, Utils.isNullOrEmpty(nullString));
		assertEquals(true, Utils.isNullOrEmpty(""));
		assertEquals(false, Utils.isNullOrEmpty("text"));
	}

	@Test
	void isNullOrEmptyText() {
		Text nullText = null;
		assertEquals(true, Utils.isNullOrEmpty(nullText));
		assertEquals(true, Utils.isNullOrEmpty(Text.empty()));
		assertEquals(false, Utils.isNullOrEmpty(Text.empty().append("text")));
	}

	@Test
	void removeGenerics() {
		String x = Utils.removeGenerics("List<String>");
		assertEquals("List", x);
		assertEquals("List", Utils.removeGenerics("List<? extends ArrayList>"));
		assertEquals("List", Utils.removeGenerics("List<String[]>"));
		assertEquals("", Utils.removeGenerics(""));
		assertEquals("", Utils.removeGenerics(null));
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
