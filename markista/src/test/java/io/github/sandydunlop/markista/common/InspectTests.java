package io.github.sandydunlop.markista.common;

import org.junit.jupiter.api.Test;

class InspectTests {
    @Test
    void inspect() {
        JreTools.inspectClass("jdk.javadoc.doclet.Doclet.Option");
    }
}
