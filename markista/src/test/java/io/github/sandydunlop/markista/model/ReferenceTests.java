package io.github.sandydunlop.markista.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceTests {
    @Test
    void test_toString() {
        Reference ref = new Reference("java.base/java.lang.String#toString()");
        assertEquals("java.base", ref.getModuleName());
        Name name = ref.getName();
        assertTrue(name.isMember());
        assertEquals("java.base/java.lang.String.toString()", ref.toString());
    }

    @Test
    void test_toString_noModule_1() {
        Reference ref = new Reference("java.lang.String#toString()");
        assertEquals("", ref.getModuleName());
        Name name = ref.getName();
        assertTrue(name.isMember());
        assertEquals("java.lang.String.toString()", ref.toString());
    }

    @Test
    void test_toString_noModule_2() {
        Reference ref = new Reference("java.lang.String#toString()");
        ref.setModule(null);
        assertNull(ref.getModuleName());
        Name name = ref.getName();
        assertTrue(name.isMember());
        assertEquals("java.lang.String.toString()", ref.toString());
    }

    @Test
    void test_toString_module_nameEmpty() {
        Reference ref = new Reference("java.base/");
        ref.setName(new Name());
        assertEquals("java.base", ref.getModuleName());
        assertEquals("java.base/", ref.toString());
    }

    @Test
    void test_toString_module_nameUnset() {
        Reference ref = new Reference("java.base/");
        assertEquals("java.base", ref.getModuleName());
        assertEquals("java.base/", ref.toString());
    }

    @Test
    void test_method_only() {
        Reference ref = new Reference("#methodname");
        assertEquals("methodname", ref.getName().simpleName());
    }

    @Test
    void test_null() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Reference(null);
        });
    }

    @Test
    void test_blank() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Reference("");
        });
    }
}
