package io.github.sandydunlop.markista.orchestration;

import io.github.qishr.cascara.lang.java.jreutil.JreUtil;
import io.github.qishr.cascara.lang.java.model.MethodNode;
import io.github.qishr.cascara.lang.java.model.JlsName;
import io.github.qishr.cascara.lang.java.model.Reference;
import io.github.qishr.cascara.lang.java.model.NameUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class JreUtilsTests {
    // @Disabled
    @Test
    void t1() {
        Reference ref = NameUtil.createReference("jdk.javadoc.doclet.Doclet.Option.Kind");
        JlsName name = ref.getName();
        Class<?> jreType = JreUtil.loadClass(name.fullyQualifiedName());
        assertNotNull(jreType);
    }

    @Test
    void test_methods() {
        Class<?> jreType = JreUtil.loadClass("java.lang.Integer");
        MethodNode[] methods = JreUtil.getMethods(jreType);
        assertNotNull(methods);
    }
}
