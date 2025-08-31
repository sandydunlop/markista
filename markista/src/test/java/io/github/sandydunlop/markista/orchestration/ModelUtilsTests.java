package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.ModelTestEnvironment;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.model.TypeReference;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ModelUtilsTests extends ModelTestEnvironment {

    @BeforeEach
    void init() {
        setupModel();
        LinkResolver.init(api, ctx);
        LinkResolver.addStandardModules();
        TextAssembler.assembleTextAndLinks(api, ctx);
        LinkResolver.setFlattenedDirectories(null);
		ctx.setModuleName("markista");
        ctx.setPackageName("io.github.sandydunlop.markista.doclet");
    }

    @Test
    void subtypes() throws NoSuchMethodException {
        Class<?> elementScannerClass = JreUtils.loadClass("javax.lang.model.util.ElementScanner6");
        TypeNode elementScanner6 = JreUtils.model(elementScannerClass);
        TypeNode scanner = newClass("Scanner", doclet);
        api.addType(scanner);

        // Set up ElementScanner6's scan method
        Method m = elementScannerClass.getMethod("scan",
                javax.lang.model.element.Element.class,
                Object.class);
        MethodNode elementScannerMethod = JreUtils.model(m);
        elementScanner6.addMethod(elementScannerMethod);

        // Set up API model version of it
        MethodNode scan = new MethodNode("void", "scan");
        ParamNode param1 = new ParamNode("javax.lang.model.element.Element", "p1");
        ParamNode param2 = new ParamNode("java.lang.Integer", "P");
        scan.addParam(param1);
        scan.addParam(param2);
        scanner.addMethod(scan);
        scan.setOwnerName(scanner.getQualifiedName());

        TypeReference typeB = TypeReference.to(elementScanner6.getQualifiedName());

        // Inheritance
        scanner.getSupertypes().add(TypeReference.to("java.lang.Object"));
        scanner.getSupertypes().add(typeB);

        String bt = ModelUtils.baseTypeName(scan);
        assertNotNull(bt);
    }
}