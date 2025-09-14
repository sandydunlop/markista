package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.Name;
import io.github.sandydunlop.markista.model.Reference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class JreUtilsTests {
    @Test
    void t1() {
        Reference ref = new Reference("jdk.javadoc.doclet.Doclet.Option.Kind");
        Name name = ref.getName();
        Class<?> jreType = JreUtils.loadClass(name.fullyQualifiedName());
        assertNotNull(jreType);
    }

    @Test
    void test_methods() {
        Class<?> jreType = JreUtils.loadClass("java.lang.Integer");
        MethodNode[] methods = JreUtils.getMethods(jreType);
        assertNotNull(methods);
    }
}
