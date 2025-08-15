package io.github.sandydunlop.markista.util;

import com.sun.source.util.DocTreePath;

import io.github.sandydunlop.markista.core.Context;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownParserTests {
    private static Context ctx;
    @Mock static Reporter reporter = new Reporter() {
        @Override
        public void print(Kind kind, String message) {
            System.out.println(kind + ": " + message);
        }

        @Override
        public void print(Kind kind, DocTreePath path, String message) {
            // Do nothing
        }

        @Override
        public void print(Kind kind, Element element, String message) {
            // Do nothing
        }
    };
    
	@BeforeAll
    static void initAll() {
		ctx =  Context.getInstance();
		ctx.setReporter(reporter);
    }

    @BeforeEach
    void init() {
        // Nothing to see here
    }

	@Test
	void resolveMarkdownLinks() {
        MarkdownParser parser = new MarkdownParser("one [link resolving][LinkResolver] two");
        MarkdownParser.Token segment = parser.firstToken();

        assertEquals(MarkdownParser.TokenKind.TEXT, segment.getKind());
        assertEquals("one ", segment.getText());
        segment = segment.getNext();

        assertEquals(MarkdownParser.TokenKind.BRACKETS_TAG, segment.getKind());
        assertEquals("link resolving", segment.getText());
        segment = segment.getNext();

        assertEquals(MarkdownParser.TokenKind.BRACKETS_TAG, segment.getKind());
        assertEquals("LinkResolver", segment.getText());
        segment = segment.getNext();

        assertEquals(MarkdownParser.TokenKind.TEXT, segment.getKind());
        assertEquals(" two", segment.getText());
        segment = segment.getNext();

        assertEquals(MarkdownParser.TokenKind.END, segment.getKind());
    }

	@Test
	void resolveMarkdownLinks_inlineCode_withBracket() {
        MarkdownParser parser = new MarkdownParser("the position of `]` within the markdown text");
        MarkdownParser.Token segment = parser.firstToken();
        assertEquals("the position of `]` within the markdown text", segment.getText());
        segment = segment.getNext();
        assertEquals(MarkdownParser.TokenKind.END, segment.getKind());
    }

    @Test
    void testEmptyString() {
        MarkdownParser parser = new MarkdownParser("");
        MarkdownParser.Token seg = parser.firstToken();
        assertEquals(MarkdownParser.TokenKind.END, seg.getKind());
    }

    @Test
    void testPlainText() {
        String markdown = "Just some plain text.";
        MarkdownParser parser = new MarkdownParser(markdown);
        MarkdownParser.Token seg = parser.firstToken();
        assertEquals(MarkdownParser.TokenKind.TEXT, seg.getKind());
        assertEquals(markdown, seg.getText());

        MarkdownParser.Token next = seg.getNext();
        assertEquals(MarkdownParser.TokenKind.END, next.getKind());
    }

    @Test
    void testBracketsTag() {
        String markdown = "Here is a [link] in text.";
        MarkdownParser parser = new MarkdownParser(markdown);

        MarkdownParser.Token seg = parser.firstToken();
        // First segment: TEXT before [
        assertEquals(MarkdownParser.TokenKind.TEXT, seg.getKind());
        assertEquals("Here is a ", seg.getText());

        seg = seg.getNext();
        // Second segment: BRACKETS_TAG
        assertEquals(MarkdownParser.TokenKind.BRACKETS_TAG, seg.getKind());
        assertEquals("link", seg.getText());

        seg = seg.getNext();
        // Third segment: TEXT after ]
        assertEquals(MarkdownParser.TokenKind.TEXT, seg.getKind());
        assertEquals(" in text.", seg.getText());

        assertEquals(MarkdownParser.TokenKind.END, seg.getNext().getKind());
    }

    @Test
    void testParensTagFollowingBrackets() {
        String markdown = "Here is [link](http://example.com)";
        MarkdownParser parser = new MarkdownParser(markdown);

        MarkdownParser.Token seg = parser.firstToken();
        // TEXT before [
        assertEquals(MarkdownParser.TokenKind.TEXT, seg.getKind());
        assertEquals("Here is ", seg.getText());

        seg = seg.getNext();
        // BRACKETS_TAG: link
        assertEquals(MarkdownParser.TokenKind.BRACKETS_TAG, seg.getKind());
        assertEquals("link", seg.getText());

        seg = seg.getNext();
        // PARENS_TAG: http://example.com
        assertEquals(MarkdownParser.TokenKind.PARENS_TAG, seg.getKind());
        assertEquals("http://example.com", seg.getText());

        assertEquals(MarkdownParser.TokenKind.END, seg.getNext().getKind());
    }

    @Test
    void testParensNotFollowingBrackets() {
        String markdown = "This (text) is not a link.";
        MarkdownParser parser = new MarkdownParser(markdown);

        MarkdownParser.Token seg = parser.firstToken();
        // The entire text including (text) should be one TEXT segment
        assertEquals(MarkdownParser.TokenKind.TEXT, seg.getKind());
        assertEquals(markdown, seg.getText());
        assertEquals(MarkdownParser.TokenKind.END, seg.getNext().getKind());
    }

    @Test
    void testCodeBlockToggle() {
        String markdown = "Text with `code [notalink]` outside.";
        MarkdownParser parser = new MarkdownParser(markdown);

        MarkdownParser.Token seg = parser.firstToken();
        // TEXT segment: "Text with `code [notalink]` outside."
        assertEquals(MarkdownParser.TokenKind.TEXT, seg.getKind());
        assertEquals("Text with `code [notalink]` outside.", seg.getText());

        assertEquals(MarkdownParser.TokenKind.END, seg.getNext().getKind());
    }

    @Test
    void testMultipleLinksAndText() {
        String markdown = "Hello [one](url1) and [two](url2)!";
        MarkdownParser parser = new MarkdownParser(markdown);

        MarkdownParser.Token seg = parser.firstToken();
        assertEquals(MarkdownParser.TokenKind.TEXT, seg.getKind());
        assertEquals("Hello ", seg.getText());

        seg = seg.getNext();
        assertEquals(MarkdownParser.TokenKind.BRACKETS_TAG, seg.getKind());
        assertEquals("one", seg.getText());

        seg = seg.getNext();
        assertEquals(MarkdownParser.TokenKind.PARENS_TAG, seg.getKind());
        assertEquals("url1", seg.getText());

        seg = seg.getNext();
        assertEquals(MarkdownParser.TokenKind.TEXT, seg.getKind());
        assertEquals(" and ", seg.getText());

        seg = seg.getNext();
        assertEquals(MarkdownParser.TokenKind.BRACKETS_TAG, seg.getKind());
        assertEquals("two", seg.getText());

        seg = seg.getNext();
        assertEquals(MarkdownParser.TokenKind.PARENS_TAG, seg.getKind());
        assertEquals("url2", seg.getText());

        seg = seg.getNext();
        assertEquals(MarkdownParser.TokenKind.TEXT, seg.getKind());
        assertEquals("!", seg.getText());

        assertEquals(MarkdownParser.TokenKind.END, seg.getNext().getKind());
    }
}

