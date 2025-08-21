package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.DirectiveNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.InterfaceTypeNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.util.LinkResolver;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.InvalidPathException;
import java.util.List;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

import jdk.javadoc.doclet.Reporter;

import com.sun.source.util.DocTreePath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    void setup() throws InvalidPathException, IOException {
        api = new Api("Test API");
        moduleNode = new ModuleNode("mod");
        api.addModule(moduleNode);

        // Add a package to namedModule (required for index.md generation)
        pkg = new PackageNode("com.example.package");
        moduleNode.addPackage(pkg);
        api.addPackage(pkg); // Package needs to be in API for LinkResolver to work

        ctx.setModuleName("");
        ctx.setReporter(reporter);
        LinkResolver.init(api, ctx);

        stringWriter = new StringWriter();

        // Stub the behavior of contextMock to return a StringWriter
        when(contextMock.createFileInModule(anyString())).thenReturn(stringWriter);
        when(contextMock.createFileInPackage()).thenReturn(stringWriter);
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
    void writeDocs_WritesModuleDocsForUnnamedModule() throws IOException {
        // Setup modules in API
        api = new Api("Test API");
        ModuleNode unnamedModule = new ModuleNode("");
        api.addModule(unnamedModule);

        // Add a package to namedModule (required for index.md generation)
        PackageNode pkg1 = new PackageNode("com.example.package");
        unnamedModule.addPackage(pkg1);
        api.addPackage(pkg1); // Package needs to be in API for LinkResolver to work

        moduleWriter.writeDocs(api);

        // Check output to contain module title
        String output = stringWriter.toString();
        assertTrue(output.contains("# Test API") || output.contains("# API"));
    }

    @Test
    void outputModuleDirectives_WritesTableForDirectiveNodes() throws IOException {
        Reference ref = Reference.to("com.example.package")
                .withLabel("com.example.package");
        DirectiveNode exportsDirective = mock(DirectiveNode.class);
        when(exportsDirective.getKind()).thenReturn(DirectiveNode.Kind.EXPORTS);        
        when(exportsDirective.getReference()).thenReturn(ref);        
        moduleNode.addDirective(exportsDirective);

        moduleWriter.writeDocs(api);

        String output = stringWriter.toString();
        assertTrue(output.contains("=== \"Exports\""));
        assertTrue(output.contains("com.example.package"));
    }

    @Test
    void outputModuleProvidesDirectives_WritesTableForProvides() throws IOException {
        InterfaceTypeNode interface0 = new InterfaceTypeNode("com.example.package.Interface", "Interface", 
                pkg.getQualifiedName());
        InterfaceTypeNode interface1 = new InterfaceTypeNode("com.example.package.Impl1", "Impl1",
                pkg.getQualifiedName());
        InterfaceTypeNode interface2 = new InterfaceTypeNode("com.example.package.Impl2", "Impl2",
                pkg.getQualifiedName());
        api.addType(interface0);
        api.addType(interface1);
        api.addType(interface2);

        Reference ref = Reference.to("com.example.package.Interface");
        ref.setKind(Reference.Kind.TYPE);
        ref.setLabel("com.example.package.Interface");
        ref.setUri("com/example/package/Interface.md");
        DirectiveNode providesDirective = mock(DirectiveNode.class);
        List<Reference> implementations = List.of(
                Reference.to("com.example.package.Impl1")
                        .withLabel("com.example.package.Impl1")
                        .withUri("com.example.package.Impl1"), 
                Reference.to("com.example.package.Impl2")
                        .withLabel("com.example.package.Impl2")
                        .withUri("com.example.package.Impl2")); 
        when(providesDirective.getImplementations()).thenReturn(implementations);
        when(providesDirective.getKind()).thenReturn(DirectiveNode.Kind.PROVIDES);
        when(providesDirective.getName()).thenReturn("com.example.package.Interface");
        when(providesDirective.getReference()).thenReturn(ref);
        moduleNode.addDirective(providesDirective);

        moduleWriter.writeDocs(api);

        String output = stringWriter.toString();
        assertTrue(output.contains("=== \"Provides\""));
        assertTrue(output.contains("com.example.package.Interface"));
        assertTrue(output.contains("com.example.package.Impl1"));
        assertTrue(output.contains("com.example.package.Impl2"));
    }

    @Test
    void outputConstantValues_WritesConstantFieldValuesPage() throws InvalidPathException, IOException {
        Reference ref = Reference.to("v");
        FieldNode fieldNode = mock(FieldNode.class);
        when(fieldNode.getModifiersString()).thenReturn("public static ");
        when(fieldNode.getSimpleName()).thenReturn("MY_CONSTANT");
        when(fieldNode.getConstantValue()).thenReturn("42");
        when(fieldNode.getConstantValueReference()).thenReturn(ref);

        moduleNode.addConstantValue(fieldNode);

        when(contextMock.createFileInModule("constant-values.md")).thenReturn(stringWriter);

        moduleWriter.writeDocs(api);

        String output = stringWriter.toString();
        assertTrue(output.contains("# Constant Field Values"));
        assertTrue(output.contains("public static"));
        assertTrue(output.contains("MY_CONSTANT"));
        assertTrue(output.contains("42"));
    }
}