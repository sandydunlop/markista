package io.github.sandydunlop.markista.orchestration;

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
}
