package io.github.sandydunlop.markista.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SourceCodeLocationTests {
    @Test
    void test() {
        SourceCodeLocation source = new SourceCodeLocation("/tmp/a.java", 10, 10);
        assertEquals("/tmp/a.java", source.getFileName());
        assertEquals(10, source.getLineNumber());
    }
}
