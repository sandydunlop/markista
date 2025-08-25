package io.github.sandydunlop.markista.common;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilsTests {
    @Disabled("WIP")
    @Test
    void test() {
        String s = Utils.simplifyNames("getReferences");
        assertEquals("getReferences", s);
    }
}
