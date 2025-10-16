package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.cascara.model.SemanticModel;
import io.github.sandydunlop.cascara.model.DirectiveNode;
import io.github.sandydunlop.cascara.model.FieldNode;
import io.github.sandydunlop.cascara.model.InterfaceNode;
import io.github.sandydunlop.cascara.model.ModuleNode;
import io.github.sandydunlop.cascara.model.NameUtil;
import io.github.sandydunlop.cascara.model.JlsName;
import io.github.sandydunlop.cascara.model.PackageNode;
import io.github.sandydunlop.cascara.model.PackageReference;
import io.github.sandydunlop.cascara.model.Reference;
import io.github.sandydunlop.cascara.model.VariableTypeNode;
import io.github.sandydunlop.markista.orchestration.LinkResolver;
import io.github.sandydunlop.cascara.model.Link;
import io.github.sandydunlop.cascara.model.ModelUtil;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.InvalidPathException;
import java.util.List;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

import jdk.javadoc.doclet.Reporter;

import com.sun.source.util.DocTreePath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModuleWriterTests {
    LinkResolver resolver;
    private static Context ctx;
    SemanticModel api;
    ModuleNode moduleNode;
    PackageNode pkg;

    private ModuleWriter moduleWriter;

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

    @AfterEach
    void resetSingleton() {
        Context.reset();
    }

    @BeforeEach
    void setup() throws InvalidPathException {
        stringWriter = new StringWriter();
        ctx = Context.getInstance();
        ctx.setWriterFactory(_ -> stringWriter);
        ctx.setReporter(reporter);
        moduleWriter = new ModuleWriter(ctx);

        api = new SemanticModel("Test API");
        moduleNode = new ModuleNode("mod");
        api.addModule(moduleNode);

        // Add a package to namedModule (required for index.md generation)
        pkg = new PackageNode("com.example.package");
        api.addPackage(pkg); // Package needs to be in API for LinkResolver to work

        PackageReference pkgRef = new PackageReference(pkg.getName().fullyQualifiedName());
        moduleNode.addPackage(pkgRef);

        ctx.setModuleName("");
        ctx.setReporter(reporter);
        resolver = new LinkResolver(api, ctx);

        stringWriter = new StringWriter();
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
        api.addPackage(pkg1); // Package needs to be in API for LinkResolver to work

        PackageReference pkgRef = new PackageReference(pkg1.getName().fullyQualifiedName());
        namedModule.addPackage(pkgRef);

        moduleWriter.writeDocs(api);

        // Check output to contain module title
        String output = stringWriter.toString();
        assertTrue(output.contains("# Module com.example.module") || output.contains("# API"));
    }

    @Test
    void writeDocs_WritesModuleDocsForUnnamedModule() throws IOException {
        // Setup modules in API
        api = new SemanticModel("Test API");
        ModuleNode unnamedModule = new ModuleNode("");
        api.addModule(unnamedModule);

        // Add a package to namedModule (required for index.md generation)
        PackageNode pkg1 = new PackageNode("com.example.package");
        api.addPackage(pkg1); // Package needs to be in API for LinkResolver to work
        PackageReference pkgRef = new PackageReference(pkg1.getName().fullyQualifiedName());
        unnamedModule.addPackage(pkgRef);

        moduleWriter.writeDocs(api);

        // Check output to contain module title
        String output = stringWriter.toString();
        assertTrue(output.contains("# Test API") || output.contains("# API"));
    }

    @Test
    void outputModuleDirectives_WritesTableForDirectiveNodes() throws IOException {
        Link ref = Link.to(NameUtil.createReference("com.example.package"));
        DirectiveNode exportsDirective = mock(DirectiveNode.class);
        when(exportsDirective.getKind()).thenReturn(DirectiveNode.Kind.EXPORTS);
        when(exportsDirective.getLink()).thenReturn(ref);
        moduleNode.addDirective(exportsDirective);

        moduleWriter.writeDocs(api);

        String output = stringWriter.toString();
        assertTrue(output.contains("=== \"Exports\""));
        assertTrue(output.contains("com.example.package"));
    }

    @Test
    void outputModuleProvidesDirectives_WritesTableForProvides() throws IOException {
        InterfaceNode interface0 = new InterfaceNode(NameUtil.createTypeName(pkg.getName(), "Interface"));
        InterfaceNode interface1 = new InterfaceNode(NameUtil.createTypeName(pkg.getName(), "Impl1"));
        InterfaceNode interface2 = new InterfaceNode(NameUtil.createTypeName(pkg.getName(), "Impl2"));
        api.addType(interface0);
        api.addType(interface1);
        api.addType(interface2);

        Link ref = Link.to(NameUtil.createReference("com.example.package.Interface"));
        ref.setKind(Link.Kind.TYPE);
        ref.setUri(URI.create("com/example/package/Interface.md"));
        DirectiveNode providesDirective = mock(DirectiveNode.class);
        List<Link> implementations = List.of(
                Link.to(NameUtil.createReference("com.example.package.Impl1")),
                Link.to(NameUtil.createReference("com.example.package.Impl2")));
        implementations.get(0).setUri(URI.create("com.example.package.Impl1"));
        implementations.get(1).setUri(URI.create("com.example.package.Impl2"));

        when(providesDirective.getImplementations()).thenReturn(implementations);
        when(providesDirective.getKind()).thenReturn(DirectiveNode.Kind.PROVIDES);
        when(providesDirective.getName()).thenReturn("com.example.package.Interface");
        when(providesDirective.getLink()).thenReturn(ref);
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
        VariableTypeNode vt = ModelUtil.parseVariableType("int");
        FieldNode fieldNode = new FieldNode(vt, NameUtil.createMemberName("MY_CONSTANT"));
        fieldNode.setConstantValue(42);
        fieldNode.setConstantValueReference(vt);
        fieldNode.addModifier(io.github.sandydunlop.cascara.model.Modifier.PUBLIC);
        fieldNode.addModifier(io.github.sandydunlop.cascara.model.Modifier.STATIC);

        moduleNode.addConstantValue(fieldNode);

        moduleWriter.writeDocs(api);

        String output = stringWriter.toString();
        assertTrue(output.contains("# Constant Field Values"));
        assertTrue(output.contains("public static"));
        assertTrue(output.contains("MY_CONSTANT"));
        assertTrue(output.contains("42"));
    }
}