package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.ModelTestEnvironment;
import io.github.sandydunlop.cascara.model.ClassNode;
import io.github.sandydunlop.cascara.model.MethodNode;
import io.github.sandydunlop.cascara.model.ModelUtil;
import io.github.sandydunlop.cascara.model.NameUtil;
import io.github.sandydunlop.cascara.model.JlsName;
import io.github.sandydunlop.cascara.model.ParamNode;
import io.github.sandydunlop.cascara.model.VariableTypeNode;
import io.github.sandydunlop.cascara.modeling.StandardModeler;
import io.github.sandydunlop.cascara.model.TypeNode;
import io.github.sandydunlop.cascara.jreutil.JreUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class JarUtilsTests extends ModelTestEnvironment {

    @BeforeEach
    void init() {
        setupModel();
        Relativizer.setFlattenedDirectories(null);
		ctx.setModuleName("markista");
        ctx.setPackageName("io.github.sandydunlop.markista.doclet");
    }

    @Test
    void getMethod_compatibleParams1() {
        ClassNode subClass = newClass("SubClass", model);
        subClass.getSupertypes().add(ModelUtil.parseVariableType("java.lang.Object"));
        subClass.getSupertypes().add(ModelUtil.parseVariableType("javax.lang.model.util.ElementScanner9"));

        JlsName methodName = NameUtil.createMemberName(subClass.getName(), "scan");
        MethodNode scanMethod = new MethodNode("void", methodName);
        scanMethod.setOwnerName(node.getName());
        subClass.addMethod(scanMethod);

        VariableTypeNode vt1 = ModelUtil.parseVariableType("javax.lang.model.element.Element");
        VariableTypeNode vt2 = ModelUtil.parseVariableType("java.lang.Integer");
        ParamNode param1 = new ParamNode(vt1, "param1");
        ParamNode param2 = new ParamNode(vt2, "param2");
        scanMethod.addParam(param1);
        scanMethod.addParam(param2);

        TextAssembler.assembleTextAndLinks(api, ctx);

        String typeName = "javax.lang.model.util.ElementScanner9";
        Class<?> standardClass = JreUtil.loadClass(typeName);

        assertNotNull(standardClass);
    }

    @Test
    void getMethod_compatibleParams2() {
        ClassNode subClass = newClass("SubClass", model);
        subClass.getSupertypes().add(ModelUtil.parseVariableType("java.lang.Object"));
        subClass.getSupertypes().add(ModelUtil.parseVariableType("java.util.ArrayList"));

        JlsName methodName = NameUtil.createMemberName(subClass.getName(), "addAll");
        MethodNode addAllMethod = new MethodNode("void", methodName);
        addAllMethod.setOwnerName(node.getName());
        subClass.addMethod(addAllMethod);

        VariableTypeNode vt1 = ModelUtil.parseVariableType("java.util.Collection<? extends String>");
        ParamNode param1 = new ParamNode(vt1, "c");
        addAllMethod.addParam(param1);

        TextAssembler.assembleTextAndLinks(api, ctx);

        String typeName = "java.util.ArrayList";
        Class<?> standardClass = JreUtil.loadClass(typeName);

        assertNotNull(standardClass);
    }

    @Test
    void methods1() {
        String typeName = "javax.lang.model.util.ElementScanner9";

        Class<?> jreClass = JreUtil.loadClass(typeName);
        StandardModeler modeller = new StandardModeler();
        TypeNode typeNode = modeller.modelClass(jreClass);
        assertNotNull(typeNode);
    }
}
