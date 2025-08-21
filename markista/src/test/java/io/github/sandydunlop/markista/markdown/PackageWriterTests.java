package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.core.Configuration;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.AnnotationTypeNode;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassTypeNode;
import io.github.sandydunlop.markista.model.EnumTypeNode;
import io.github.sandydunlop.markista.model.InterfaceTypeNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.util.LinkResolver;
import io.github.sandydunlop.markista.util.TextAssembler;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.InvalidPathException;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PackageWriterTests {
    Api api;
    ModuleNode moduleNode;
    PackageNode packageNode;
    PackageNode modelPackage;

    Writer writer;

    // Mockito will now call the new constructor with this mock
    @InjectMocks
    PackageWriter packageWriter;

    @Mock
    private Context contextMock;

    @BeforeAll
    static void initAll() {
        // Nothing to see here
    }

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
    
    @BeforeEach
    void setup() throws IOException {
        api = new Api("Test API");

        // Mock Writer to capture written content
        writer = new StringWriter();
        when(contextMock.createFileInPackage()).thenReturn(writer);

        moduleNode = new ModuleNode("markista");
        packageNode = new PackageNode("io.github.sandydunlop.markista");
        modelPackage = new PackageNode("io.github.sandydunlop.markista.model");

        Text body = Text.empty();
        body.append(Text.Segment.empty()
                .setKind(Text.SegmentKind.TEXT)
                .setText("This is a test class [Node](Node)."));
        ClassTypeNode nodeClass = new ClassTypeNode("io.github.sandydunlop.markista.model.Node", "Node", 
                packageNode.getQualifiedName());
        nodeClass.setBody(body);
        nodeClass.setFullBody(body);
        nodeClass.setFirstSentence(body);
        
        modelPackage.addType(nodeClass);
        packageNode.addPackage(modelPackage);
        moduleNode.addPackage(packageNode);
        moduleNode.addPackage(modelPackage);

        api.addModule(moduleNode);
        api.addPackage(packageNode);
        api.addPackage(modelPackage);
        api.addType(nodeClass);
		LinkResolver.init(api, contextMock);
		LinkResolver.setFlattenedDirectories(null);
    }

    void setupLinks() {
        Context ctx = Context.getInstance();
        ctx.setApi(api);
        LinkResolver.init(api, ctx);
        LinkResolver.addNativeModules();
        TextAssembler.assembleTextAndLinks(api, ctx);
    }

    @Test
    void packageWriter_outputs_nestedPackages_withoutTabs() throws IOException {
        setupLinks();

        Configuration.setUseContentTabs(false);

        packageWriter.outputPackageDoc(packageNode);
        String output = writer.toString();
        assertTrue(output.contains("Packages"));
        assertTrue(output.contains("[model](model/index.md)"));
    }

    @Test
    void packageWriter_outputs_nestedPackages_withTabs() throws IOException {
        setupLinks();

        Configuration.setUseContentTabs(true);

        packageWriter.outputPackageDoc(packageNode);
        String output = writer.toString();
        assertTrue(output.contains("Packages"));
        assertTrue(output.contains("[model](model/index.md)"));
    }

    @Test
    void packagrWiter_outputsPackageMembers_withoutTabs() throws InvalidPathException, IOException {
        EnumTypeNode enumNode = new EnumTypeNode("io.github.sandydunlop.markista.model.TestEnum", "TestEnum", modelPackage.getModuleName());
        InterfaceTypeNode interfaceNode = new InterfaceTypeNode("io.github.sandydunlop.markista.model.TestInterface", "TestInterface", modelPackage.getModuleName());
        AnnotationTypeNode annotationNode = new AnnotationTypeNode("io.github.sandydunlop.markista.model.TestAnnotation", "TestAnnotation", modelPackage.getModuleName());
        modelPackage.addType(enumNode);
        modelPackage.addType(interfaceNode);
        modelPackage.addType(annotationNode);
        api.addType(enumNode);
        api.addType(interfaceNode);
        api.addType(annotationNode);
        setupLinks();
        when(contextMock.getApi()).thenReturn(api);

        Configuration.setUseContentTabs(false);

        packageWriter.outputPackageDoc(modelPackage);
        String output = writer.toString();

        assertTrue(output.contains("[Node](Node.md)"));
        assertTrue(output.contains("[TestInterface](TestInterface.md)"));
        assertTrue(output.contains("[TestEnum](TestEnum.md)"));
        assertTrue(output.contains("[TestAnnotation](TestAnnotation.md)"));

        assertTrue(output.contains("Enum Class TestEnum"));
        assertTrue(output.contains("Interface TestInterface"));
        assertTrue(output.contains("Annotation Interface TestAnnotation"));
    }

    @Test
    void packagrWiter_outputsPackageMembers_withTabs() throws InvalidPathException, IOException {
        EnumTypeNode enumNode = new EnumTypeNode("io.github.sandydunlop.markista.model.TestEnum", "TestEnum", modelPackage.getModuleName());
        InterfaceTypeNode interfaceNode = new InterfaceTypeNode("io.github.sandydunlop.markista.model.TestInterface", "TestInterface", modelPackage.getModuleName());
        AnnotationTypeNode annotationNode = new AnnotationTypeNode("io.github.sandydunlop.markista.model.TestAnnotation", "TestAnnotation", modelPackage.getModuleName());
        modelPackage.addType(enumNode);
        modelPackage.addType(interfaceNode);
        modelPackage.addType(annotationNode);
        api.addType(enumNode);
        api.addType(interfaceNode);
        api.addType(annotationNode);
        setupLinks();
        when(contextMock.getApi()).thenReturn(api);

        Configuration.setUseContentTabs(true);
        
        packageWriter.outputPackageDoc(modelPackage);
        String output = writer.toString();

        assertTrue(output.contains("[Node](Node.md)"));
        assertTrue(output.contains("[TestInterface](TestInterface.md)"));
        assertTrue(output.contains("[TestEnum](TestEnum.md)"));
        assertTrue(output.contains("[TestAnnotation](TestAnnotation.md)"));

        assertTrue(output.contains("Enum Class TestEnum"));
        assertTrue(output.contains("Interface TestInterface"));
        assertTrue(output.contains("Annotation Interface TestAnnotation"));
    }
}

