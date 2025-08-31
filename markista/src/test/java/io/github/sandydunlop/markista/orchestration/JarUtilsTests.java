package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.ModelTestEnvironment;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.TypeReference;
import io.github.sandydunlop.markista.modelling.StandardModeller;
import io.github.sandydunlop.markista.model.TypeNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class JarUtilsTests extends ModelTestEnvironment {

    @BeforeEach
    void init() {
        setupModel();
        LinkResolver.init(api, ctx);
        LinkResolver.addStandardModules();
        LinkResolver.setFlattenedDirectories(null);
		ctx.setModuleName("markista");
        ctx.setPackageName("io.github.sandydunlop.markista.doclet");
    }

    @Test
    void inheritedMethodWithIdenticalTypeParam() {
        String typeName = "javax.lang.model.util.ElementScanner9";

        Class<?> standardClass = JreUtils.loadClass(typeName);
        MethodNode[] methods = JreUtils.getMethods(standardClass);
        assertNotNull(methods);
    }

    @Test
    void getMethod_compatibleParams1() {
        ClassNode subClass = newClass("SubClass", model);
        subClass.getSupertypes().add(TypeReference.to("java.lang.Object"));
        subClass.getSupertypes().add(TypeReference.to("javax.lang.model.util.ElementScanner9"));

        MethodNode scanMethod = new MethodNode("void", "scan");
        scanMethod.setOwnerName(node.getQualifiedName());
        subClass.addMethod(scanMethod);

        ParamNode param1 = new ParamNode("javax.lang.model.element.Element", "param1");
        ParamNode param2 = new ParamNode("java.lang.Integer", "param2");
        scanMethod.addParam(param1);
        scanMethod.addParam(param2);

        TextAssembler.assembleTextAndLinks(api, ctx);

        String typeName = "javax.lang.model.util.ElementScanner9";
        Class<?> standardClass = JreUtils.loadClass(typeName);

        MethodNode[] methods = JreUtils.getMethods(standardClass);
        assertNotNull(methods);
    }

    @Test
    void getMethod_compatibleParams2() {
        ClassNode subClass = newClass("SubClass", model);
        subClass.getSupertypes().add(TypeReference.to("java.lang.Object"));
        subClass.getSupertypes().add(TypeReference.to("java.util.ArrayList"));

        MethodNode addAllMethod = new MethodNode("void", "addAll");
        addAllMethod.setOwnerName(node.getQualifiedName());
        subClass.addMethod(addAllMethod);

        ParamNode param1 = new ParamNode("java.util.Collection<? extends String>", "c");
        addAllMethod.addParam(param1);

        TextAssembler.assembleTextAndLinks(api, ctx);

        String typeName = "java.util.ArrayList";
        Class<?> standardClass = JreUtils.loadClass(typeName);

        MethodNode[] methods = JreUtils.getMethods(standardClass);
        assertNotNull(methods);
    }

    @Test
    void methods1() {
        String typeName = "javax.lang.model.util.ElementScanner9";

        Class<?> jreClass = JreUtils.loadClass(typeName);
        StandardModeller modeller = new StandardModeller();
        TypeNode typeNode = modeller.modelClass(jreClass);
        assertNotNull(typeNode);
    }
}
