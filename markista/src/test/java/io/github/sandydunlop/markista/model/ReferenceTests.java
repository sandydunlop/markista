package io.github.sandydunlop.markista.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceTests {
    @Test
    void anchor() {
        Reference ref = Reference.to("Node");
        assertFalse(ref.hasAnchor());
        ref.setAnchor("#a");
        ref.setHasAnchor(true);
        assertTrue(ref.hasAnchor());
    }    
}
