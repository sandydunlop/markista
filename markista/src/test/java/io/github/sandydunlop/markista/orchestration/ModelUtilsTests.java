package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.ModelTestEnvironment;
import io.github.sandydunlop.cascara.model.MethodNode;
import io.github.sandydunlop.cascara.model.ModelUtil;
import io.github.sandydunlop.cascara.model.NameUtil;
import io.github.sandydunlop.cascara.model.JlsName;
import io.github.sandydunlop.cascara.model.ParamNode;
import io.github.sandydunlop.cascara.model.TypeNode;
import io.github.sandydunlop.cascara.model.VariableTypeNode;
import io.github.sandydunlop.cascara.modeling.StandardModeler;
import io.github.sandydunlop.cascara.jreutil.JreUtil;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ModelUtilsTests extends ModelTestEnvironment {

    @BeforeEach
    void init() {
        setupModel();
        TextAssembler.assembleTextAndLinks(api, ctx);
        Relativizer.setFlattenedDirectories(null);
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
        ParamNode param1 = new ParamNode(vt1, "p1");
        ParamNode param2 = new ParamNode(vt2, "P");
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