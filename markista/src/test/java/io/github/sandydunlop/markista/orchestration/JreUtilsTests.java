package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.cascara.model.MethodNode;
import io.github.sandydunlop.cascara.model.Name;
import io.github.sandydunlop.cascara.model.Reference;
import io.github.sandydunlop.cascara.jreutil.JreUtil;
import io.github.sandydunlop.cascara.model.ModelUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class JreUtilsTests {
    @Test
    void t1() {
        Reference ref = ModelUtil.createReference("jdk.javadoc.doclet.Doclet.Option.Kind");
        Name name = ref.getName();
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
