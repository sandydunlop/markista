package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.core.Context;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilsTests {
    @Disabled("WIP")
    @Test
    void test() {
        String s = Context.NameSimplifier.simplifyNames("getReferences");
        assertEquals("getReferences", s);
    }
}
