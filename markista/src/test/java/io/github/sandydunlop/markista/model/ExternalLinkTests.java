package io.github.sandydunlop.markista.model;

import io.github.sandydunlop.markista.core.Context;

import java.net.URI;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExternalLinkTests {
    Context ctx = Context.getInstance();

    @Test
    void test() {
        ExternalLink ext = new ExternalLink(ctx, URI.create("../other.module"));
        assertEquals("../other.module", ext.getUri().toString());
    }

    @Test
    void standard_module() {
        ExternalLink ext = new ExternalLink(ctx, URI.create("https://docs.oracle.com/en/java/javase/21/docs/api/"));
        // https://docs.oracle.com/en/java/javase/21/docs/api/element-list
        ext.load();
        assertNotNull(ext.getApi());
    }
}
