package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.ModelTestEnvironment;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.Link;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.Text.Segment;
import io.github.sandydunlop.markista.orchestration.LinkResolver;
import io.github.sandydunlop.markista.orchestration.TextAssembler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownUtilsTests extends ModelTestEnvironment {
    @BeforeEach
    void setup() {
        setupModel();
		ctx.setModuleName("markista");
        ctx.setPackageName("io.github.sandydunlop.markista.doclet");
        // TypeUtils.init(api, null);
        LinkResolver.init(api, ctx);
    }

    @Test
    void formatReference_URL() {
        Link link = new Link().withKind(Link.Kind.URL).withUri("http://example.com");
        Segment segment = Segment.empty().setLink(link).setText("http://example.com");
        String markdown = MarkdownUtils.formatLink(segment, false, false);
        assertEquals("[http://example.com](http://example.com)", markdown);
    }



    @Test
    void formatLink_URL() {
        Link link = new Link(Link.Kind.URL, null, "http://example.com");
        Text.Segment segment = Text.Segment.empty()
                .setKind(Segment.Kind.LINK)
                .setLink(link);
        LinkResolver.resolve(link);
        String markdown = MarkdownUtils.formatLink(segment, false, false);
        assertEquals("[http://example.com](http://example.com)", markdown);
    }

    @Test
    void formatLink_PACKAGE_qualified() {
        Link link = Link.to("io.github.sandydunlop.markista.model")
                .withKind(Link.Kind.PACKAGE);
        Text.Segment segment = Text.Segment.empty()
                .setKind(Segment.Kind.LINK)
                .setLink(link);
        LinkResolver.resolve(link);
        String markdown = MarkdownUtils.formatLink(segment, false, false);
        assertEquals("[io.github.sandydunlop.markista.model](../model/index.md)", markdown);
    }

    @Test
    void formatLink_PACKAGE_unqualified() {
        Link link = Link.to("model")
                .withKind(Link.Kind.PACKAGE);
        Text.Segment segment = Text.Segment.empty()
                .setKind(Segment.Kind.LINK)
                .setLink(link);
        LinkResolver.resolve(link);
        String markdown = MarkdownUtils.formatLink(segment, false, false);
        assertEquals("[io.github.sandydunlop.markista.model](../model/index.md)", markdown);
    }

    @Test
    void formatLink_PRIMITIVE() {
        Link link = new Link(Link.Kind.PRIMITIVE, "int", null);
        Text.Segment segment = Text.Segment.empty()
                .setKind(Segment.Kind.LINK)
                .setLink(link);
        LinkResolver.resolve(link);
        String markdown = MarkdownUtils.formatLink(segment, false, false);
        assertEquals("int", markdown);
    }
}
