package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.ModelTestEnvironment;
import io.github.qishr.cascara.lang.java.util.JreUtil;
import io.github.qishr.cascara.lang.java.model.MethodNode;
import io.github.qishr.cascara.lang.java.model.ModelUtil;
import io.github.qishr.cascara.lang.java.model.NameUtil;
import io.github.qishr.cascara.lang.java.model.JlsName;
import io.github.qishr.cascara.lang.java.model.ParamNode;
import io.github.qishr.cascara.lang.java.model.TypeNode;
import io.github.qishr.cascara.lang.java.model.VariableTypeNode;
import io.github.qishr.cascara.lang.java.modeler.StandardModeler;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ModelUtilsTests extends ModelTestEnvironment {

    @BeforeEach
    void init() {
        setupModel();
        TextAssembler.assembleTextAndLinks(api, ctx);
        // Relativizer.setFlattenedDirectories(null);
		ctx.setModuleName("markista");
        ctx.setPackageName("io.github.sandydunlop.markista.doclet");
    }

    @Test
    void subtypes() throws NoSuchMethodException {
        StandardModeler modeller = new StandardModeler();
        Class<?> elementScannerClass = JreUtil.loadClass("javax.lang.model.util.ElementScanner6");
        TypeNode elementScanner6 = modeller.modelType(elementScannerClass);
        TypeNode scanner = newClass("Scanner", doclet);
        api.addType(scanner);

        // Set up ElementScanner6's scan method
        Method m = elementScannerClass.getMethod("scan",
                javax.lang.model.element.Element.class,
                Object.class);
        MethodNode elementScannerMethod = modeller.modelMethod(m);
        elementScanner6.addMethod(elementScannerMethod);

        // Set up API model version of it
        JlsName methodName = NameUtil.createMemberName(scanner.getName(), "scan");
        MethodNode scan = new MethodNode("void", methodName);
        VariableTypeNode vt1 = ModelUtil.parseVariableType("javax.lang.model.element.Element");
        VariableTypeNode vt2 = ModelUtil.parseVariableType("java.lang.Integer");
        ParamNode param1 = new ParamNode(vt1, NameUtil.createMemberName("p1"));
        ParamNode param2 = new ParamNode(vt2, NameUtil.createMemberName("P"));
        scan.addParam(param1);
        scan.addParam(param2);
        scanner.addMethod(scan);
        scan.setOwnerName(scanner.getName());

        VariableTypeNode typeB = ModelUtil.parseVariableType(elementScanner6.getName().fullyQualifiedName());

        // Inheritance
        scanner.getSupertypes().add(ModelUtil.parseVariableType("java.lang.Object"));
        scanner.getSupertypes().add(typeB);

        String bt = ModelUtils.baseTypeName(scan);
        assertNotNull(bt);
    }
}