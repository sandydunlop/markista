package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.ModelTestEnvironment;
import io.github.qishr.cascara.lang.java.model.Link;
import io.github.qishr.cascara.lang.java.model.Text;
import io.github.qishr.cascara.lang.java.model.Text.Segment;
import io.github.qishr.cascara.lang.java.model.NameUtil;
import io.github.sandydunlop.markista.orchestration.LinkResolver;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownUtilsTests extends ModelTestEnvironment {
    @BeforeEach
    void setup() {
        setupModel();
		ctx.setModuleName("markista");
        ctx.setPackageName("io.github.sandydunlop.markista.doclet");
        resolver = new LinkResolver(api, ctx);
    }

    @Test
    void formatReference_URL() {
        Link link = Link.toWeb(URI.create("http://example.com"));
        Segment segment = Segment.empty().setLink(link).setText("http://example.com");
        String markdown = MarkdownUtils.formatLink(segment.getLink(), "http://example.com");
        assertEquals("[http://example.com](http://example.com)", markdown);
    }



    @Test
    void formatLink_URL() {
        Link link = Link.toWeb(URI.create("http://example.com"));
        Text.Segment segment = Text.Segment.empty()
                .setKind(Segment.Kind.LINK)
                .setLink(link);
        resolver.resolveLink(link);
        String markdown = MarkdownUtils.formatLink(segment.getLink(), "http://example.com");
        assertEquals("[http://example.com](http://example.com)", markdown);
    }

    @Test
    void formatLink_PACKAGE_qualified() {
        Link link = Link.to(NameUtil.createReference("io.github.sandydunlop.markista.model"))
                .withKind(Link.Kind.PACKAGE);
        Text.Segment segment = Text.Segment.empty()
                .setKind(Segment.Kind.LINK)
                .setLink(link)
                .setText("io.github.sandydunlop.markista.model");
        resolver.resolveLink(link);
        String markdown = MarkdownUtils.formatLink(segment.getLink(), "io.github.sandydunlop.markista.model");
        assertEquals("[io.github.sandydunlop.markista.model](../model/index.md)", markdown);
    }

    @Test
    void formatLink_PRIMITIVE() {
        Link link = Link.to(NameUtil.createReference("int")).withKind(Link.Kind.PRIMITIVE);
        Text.Segment segment = Text.Segment.empty()
                .setText("int")
                .setKind(Segment.Kind.LINK)
                .setLink(link);
        resolver.resolveLink(link);
        String markdown = MarkdownUtils.formatLink(segment.getLink(), "int");
        assertEquals("int", markdown);
    }
}
