package io.github.sandydunlop.markista.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextTests {
    
    @Test
    void subtext_oneParam()  {
        Text text = Text.of("one");
        text.append("two");
        text.append("three");

        assertEquals(3, text.getSegments().size());

        Text one = text.subtext(0, 0);
        assertEquals("one", one.toString());
    }

    @Test
    void subtext_twoParams()  {
        Text.Segment seg = Text.Segment.empty().setText("one");
        Text text = Text.of(seg);
        text.append("two");
        text.append("three");

        assertEquals(3, text.getSegments().size());

        Text twoThree = text.subtext(1);
        assertEquals("twothree", twoThree.toString());
    }

    @Test
    void linkLabel_empty() {
        Link link = Link.to("java.lang.String").withLabel("String");
        Text.Segment seg = Text.Segment.empty().setLink(link);
        seg.setText("");
        assertEquals("String", seg.toString());
    }

    @Test
    void test_isEmpty() {
        Text text = Text.empty();
        assertTrue(text.isEmpty());
    }

    @Test
    void test_null() {
        Link link = Link.to("java.lang.String").withLabel("String");
        Text.Segment seg = Text.Segment.empty().setLink(link);
        seg.setText(null);
        assertEquals("String", seg.toString());
    }

    @Test
    void test_append_empty() {
        Text text;
        Text.Segment seg = Text.Segment.empty();

        seg.setKind(Text.Segment.Kind.INHERIT);
        text = Text.empty().append(seg);
        assertEquals(1, text.getSegments().size());

        seg.setKind(Text.Segment.Kind.TEXT);
        text = Text.empty().append(seg);
        assertEquals(0, text.getSegments().size());
    }
}
