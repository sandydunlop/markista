package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.model.*;
import io.github.sandydunlop.markista.util.Context;
import io.github.sandydunlop.markista.util.LinkResolver;
import jdk.javadoc.doclet.Reporter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.source.util.DocTreePath;

import java.io.*;
import java.util.List;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModuleWriterTests {
    private static Context ctx;
    Api api;
    ModuleNode moduleNode;
    PackageNode pkg;

    // Mockito will now call the new constructor with this mock
    @InjectMocks
    private ModuleWriter moduleWriter;

    @Mock
    private Context contextMock;

    // This is the writer we will use to capture output
    private StringWriter stringWriter;

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
		ctx =  Context.getInstance();
		ctx.setReporter(reporter);
    }

    @BeforeEach
    void setup() throws IOException {
        api = new Api();
        moduleNode = new ModuleNode("mod");
        api.addModule(moduleNode);

        // Add a package to namedModule (required for index.md generation)
        pkg = new PackageNode("com.example.package");
        moduleNode.addPackage(pkg);
        api.addPackage(pkg); // Package needs to be in API for LinkResolver to work

        ctx.setModuleName("");
        ctx.setReporter(reporter);
        LinkResolver.init(api);

        stringWriter = new StringWriter();

        // Stub the behavior of fileUtilsMock to return a StringWriter
        when(contextMock.createModuleFile(anyString())).thenReturn(stringWriter);
    }

    @Test
    void writeDocs_WritesModuleDocsForAllModules() throws IOException {
        // Setup modules in API
        ModuleNode namedModule = new ModuleNode("com.example.module");
        ModuleNode unnamedModule = new ModuleNode("");
        api.addModule(namedModule);
        api.addModule(unnamedModule);

        // Add a package to namedModule (required for index.md generation)
        PackageNode pkg1 = new PackageNode("com.example.package");
        namedModule.addPackage(pkg1);
        api.addPackage(pkg1); // Package needs to be in API for LinkResolver to work

        moduleWriter.writeDocs(api);

        // Check output to contain module title
        String output = stringWriter.toString();
        assertTrue(output.contains("# Module com.example.module") || output.contains("# API"));
    }

    @Test
    void outputModuleDirectives_WritesTableForDirectiveNodes() throws IOException {
        DirectiveNode exportsDirective = mock(DirectiveNode.class);
        when(exportsDirective.getName()).thenReturn("com.example.package");
        when(exportsDirective.getKind()).thenReturn(DirectiveNode.Kind.EXPORTS);        
        moduleNode.addDirective(exportsDirective);

        moduleWriter.writeDocs(api);

        String output = stringWriter.toString();
        assertTrue(output.contains("=== \"Exports\""));
        assertTrue(output.contains("com.example.package"));
    }

    @Test
    void outputModuleProvidesDirectives_WritesTableForProvides() throws IOException {
        InterfaceNode interface0 = new InterfaceNode("com.example.package.Interface", "Interface", pkg);
        InterfaceNode interface1 = new InterfaceNode("com.example.package.Impl1", "Impl1", pkg);
        InterfaceNode interface2 = new InterfaceNode("com.example.package.Impl2", "Impl2", pkg);
        api.addInterface(interface0);
        api.addInterface(interface1);
        api.addInterface(interface2);

        DirectiveNode providesDirective = mock(DirectiveNode.class);
        when(providesDirective.getInterface()).thenReturn("com.example.package.Interface");
        List<String> implementations = List.of("com.example.package.Impl1", "com.example.package.Impl2");
        when(providesDirective.getImplementations()).thenReturn(implementations);
        when(providesDirective.getKind()).thenReturn(DirectiveNode.Kind.PROVIDES);
        moduleNode.addDirective(providesDirective);

        moduleWriter.writeDocs(api);

        String output = stringWriter.toString();
        assertTrue(output.contains("=== \"Provides\""));
        assertTrue(output.contains("com.example.package.Interface"));
        assertTrue(output.contains("com.example.package.Impl1"));
        assertTrue(output.contains("com.example.package.Impl2"));
    }

    @Test
    void outputConstantValues_WritesConstantFieldValuesPage() throws IOException {
        FieldNode fieldNode = mock(FieldNode.class);
        when(fieldNode.getModifiersString()).thenReturn("public static ");
        TypeNode typeNode = mock(TypeNode.class);
        when(typeNode.getQualifiedName()).thenReturn("java.lang.String");
        when(fieldNode.getType()).thenReturn(typeNode);
        when(fieldNode.getSimpleName()).thenReturn("MY_CONSTANT");
        when(fieldNode.getConstantValue()).thenReturn("42");

        moduleNode.addConstantValue(fieldNode);

        when(contextMock.createModuleFile("constant-values.md")).thenReturn(stringWriter);

        moduleWriter.writeDocs(api);

        String output = stringWriter.toString();
        assertTrue(output.contains("# Constant Field Values"));
        assertTrue(output.contains("public static"));
        assertTrue(output.contains("MY_CONSTANT"));
        assertTrue(output.contains("42"));
    }
}