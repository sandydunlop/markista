package io.github.sandydunlop.markista.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;

import java.nio.file.Path;
import java.util.List;

import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ModuleNode;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.element.ModuleElement.DirectiveKind;
import javax.lang.model.element.ModuleElement.ExportsDirective;
import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;
import javax.tools.Diagnostic.Kind;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

class ApiScannerTests {
    private static Context ctx;

    @Mock static Reporter reporter = new Reporter() {
        @Override
        public void print(Kind kind, String message) {
            System.out.println(kind + ": " + message);
        }

        @Override
        public void print(Kind kind, DocTreePath path, String message) {
            // Do nothing
        }

        @Override
        public void print(Kind kind, Element element, String message) {
            // Do nothing
        }
    };
    
    @BeforeAll
    static void initAll() {
        ctx = Context.getInstance();
		ctx.setReporter(reporter);
        ctx.setOutputDirectory("/tmp/doc");
    }

    @BeforeEach
    void init() {
        // Nothing to see here
    }

    @Test
    void visitModule_named() {
        final String MODULE_NAME = "module.name";
        final String MODULE_NAME2 = "module.two";
        final String PACKAGE_NAME = "package.name";

        Name moduleName = mock(Name.class);
        Mockito.when(moduleName.toString()).thenReturn(MODULE_NAME);
        ModuleElement moduleElement = mock(ModuleElement.class);
        Mockito.when(moduleElement.getQualifiedName()).thenReturn(moduleName);

        Name moduleName2 = mock(Name.class);
        Mockito.when(moduleName2.toString()).thenReturn(MODULE_NAME2);
        ModuleElement moduleElement2 = mock(ModuleElement.class);
        Mockito.when(moduleElement2.getQualifiedName()).thenReturn(moduleName2);

        DocTrees docTrees = mock(DocTrees.class);
        DocletEnvironment mockEnvironment = mock(DocletEnvironment.class);
        Mockito.when(mockEnvironment.getDocTrees()).thenReturn(docTrees);
        TypeUtils.init(null, mockEnvironment);

        JavaFileObject jfo = mock(JavaFileObject.class);
        Mockito.when(jfo.getName()).thenReturn("module-info.java");
        Mockito.when(jfo.toUri()).thenReturn(Path.of("").toUri());
        Elements elementUtils = mock(Elements.class);
        Mockito.when(elementUtils.getFileObjectOf(any())).thenReturn(jfo);
        Mockito.when(mockEnvironment.getElementUtils()).thenReturn(elementUtils);

        Name packageName = mock(Name.class);
        Mockito.when(packageName.toString()).thenReturn(PACKAGE_NAME);
        PackageElement packageElement = mock(PackageElement.class);
        Mockito.when(packageElement.getQualifiedName()).thenReturn(packageName);

        ExportsDirective directive = mock(ExportsDirective.class);
        Mockito.when(directive.getKind()).thenReturn(DirectiveKind.EXPORTS);
        Mockito.when(directive.getPackage()).thenReturn(packageElement);
        Mockito.when(directive.getTargetModules()).thenAnswer(_ -> List.of(moduleElement2));

        Mockito.when(moduleElement.getDirectives()).thenAnswer(_ -> List.of(directive));

        ApiScanner scanner = new ApiScanner(mockEnvironment);
        scanner.visitModule(moduleElement, Integer.valueOf(0));

        Api api = scanner.api;
        assertEquals(1, api.getModules().size());
        ModuleNode moduleNode = api.getModules().get(0);
        assertEquals(MODULE_NAME, moduleNode.getName());
    }
    
    @Test
    void visitModule_unnamed() {
        Name moduleName = mock(Name.class);
        Mockito.when(moduleName.toString()).thenReturn("");
        ModuleElement moduleElement = mock(ModuleElement.class);
        Mockito.when(moduleElement.getQualifiedName()).thenReturn(moduleName);

        DocTrees docTrees = mock(DocTrees.class);
        DocletEnvironment mockEnvironment = mock(DocletEnvironment.class);
        Mockito.when(mockEnvironment.getDocTrees()).thenReturn(docTrees);
        TypeUtils.init(null, mockEnvironment);

        ApiScanner scanner = new ApiScanner(mockEnvironment);
        scanner.visitModule(moduleElement, Integer.valueOf(0));

        Api api = scanner.api;
        assertEquals(0, api.getModules().size());
    }
}
