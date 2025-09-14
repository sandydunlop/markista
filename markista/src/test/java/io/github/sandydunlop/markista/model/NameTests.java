package io.github.sandydunlop.markista.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameTests {
    @Test
    void test_components_size() {
        Name name = new Name("io.github");
        assertEquals(2, name.componentCount());
        name = new Name("io.github.sandydunlop");
        assertEquals(3, name.componentCount());
        name = new Name("io.github.sandydunlop.markista");
        assertEquals(4, name.componentCount());
    }

    @Test
    void test_components_first() {
        Name name = new Name("io.github.sandydunlop.markista");
        assertEquals("io", name.firstComponents(1).toString());
        name = new Name("io.github.sandydunlop.markista");
        assertEquals("io.github", name.firstComponents(2).toString());
        name = new Name("io.github.sandydunlop.markista");
        assertEquals("io.github.sandydunlop", name.firstComponents(3).toString());
    }

    @Test
    void test_components_last() {
        Name name = new Name("io.github.sandydunlop.markista");
        assertEquals("markista", name.lastComponents(1).toString());
        name = new Name("io.github.sandydunlop.markista");
        assertEquals("sandydunlop.markista", name.lastComponents(2).toString());
        name = new Name("io.github.sandydunlop.markista");
        assertEquals("github.sandydunlop.markista", name.lastComponents(3).toString());
    }

    @Test
    void test_simple() {
        Name name = new Name("i.g.s.m");
        assertEquals("m", name.simpleName());
        name = new Name("i.g.s");
        assertEquals("s", name.simpleName());
    }

    @Test
    void test_fullyQualified() {
        Name name = new Name("i.g.s.m");
        assertEquals("i.g.s.m", name.fullyQualifiedName());
        name = new Name("i.g.s");
        assertEquals("i.g.s", name.fullyQualifiedName());
    }

    @Test
    void test_name_kinds0() {
        Name name = new Name("p.A.B.C");
        name.setPackageComponentCount(1);
        assertEquals("A.B.C", name.typeName().toString());
        assertEquals("C", name.simpleName());
        assertEquals("p.A.B.C", name.fullyQualifiedName());
        assertEquals("A$B$C", name.simpleBinaryName());
        assertEquals("p.A$B$C", name.fullyQualifiedBinaryName());
        assertEquals("p", name.packageName().toString());
    }

    @Test
    void test_name_kinds() {
        Name name = new Name("p.A.B.C", "p");
        assertEquals("A.B.C", name.typeName().toString());
        assertEquals("C", name.simpleName());
        assertEquals("p.A.B.C", name.fullyQualifiedName());
        assertEquals("A$B$C", name.simpleBinaryName());
        assertEquals("p.A$B$C", name.fullyQualifiedBinaryName());
        assertEquals("p", name.packageName().toString());
    }

    @Test
    void test_name_of_method() {
        Name name = new Name("p.A.m");
        name.setPackageComponentCount(1);
        assertEquals("m", name.simpleName());
        assertEquals("p.A.m", name.fullyQualifiedName());
    }

    @Test
    void test_name_first_negative() {
        Name name = new Name("io.github.sandydunlop.markista");
        assertEquals("io", name.firstComponents(-3).toString());
        assertEquals("io.github", name.firstComponents(-2).toString());
        assertEquals("io.github.sandydunlop", name.firstComponents(-1).toString());
    }

    @Test
    void test_name_last_negative() {
        Name name = new Name("io.github.sandydunlop.markista");
        assertEquals("markista", name.lastComponents(-3).toString());
        assertEquals("sandydunlop.markista", name.lastComponents(-2).toString());
        assertEquals("github.sandydunlop.markista", name.lastComponents(-1).toString());
    }

    @Test
    void parts1() {
        Name name = new Name("member", "com.example.Type", "com.example");
        assertEquals("com.example", name.packageName().toString());
        assertEquals("Type", name.typeName().toString());
        assertEquals("member", name.memberName().toString());
    }

    @Test
    void parts2() {
        Name name = new Name("member", "com.example.Type", "com.example");
        Name typeName = name.typeName();
        Name memberName = name.memberName();

        assertTrue(name.isMember());
        assertTrue(typeName.isType());
        assertTrue(memberName.isMember());
    }

    @Test
    void parts_packageName_hasNo_typeName() {
        Name name = new Name(null, "io.github.sandydunlop.markista");
        assertTrue(name.isPackage());
        Name typeName = name.typeName();
        assertTrue(typeName.isEmpty());
    }

    @Test
    void parts_packageName_hasNo_typeName2() {
        Name name = new Name(null, "sandydunlop.markista");
        assertTrue(name.isPackage());
        Name typeName = name.typeName();
        assertTrue(typeName.isEmpty());
    }

    @Test
    void parts_packageName_hasNo_memberName() {
        Name name = new Name(null, "io.github.sandydunlop.markista");
        assertTrue(name.isPackage());
        Name memberName = name.memberName();
        assertTrue(memberName.isEmpty());
    }

    @Test
    void parts_last_negative() {
        Name name = new Name(null, "io.github.sandydunlop.markista");
        Name last = name.lastComponents(-2);
        assertTrue(last.isPackage());
    }
}
