package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.core.Configuration;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.cascara.model.AnnotationNode;
import io.github.sandydunlop.cascara.model.Api;
import io.github.sandydunlop.cascara.model.ClassNode;
import io.github.sandydunlop.cascara.model.EnumNode;
import io.github.sandydunlop.cascara.model.InterfaceNode;
import io.github.sandydunlop.cascara.model.ModelUtil;
import io.github.sandydunlop.cascara.model.ModuleNode;
import io.github.sandydunlop.cascara.model.Name;
import io.github.sandydunlop.cascara.model.PackageNode;
import io.github.sandydunlop.cascara.model.PackageReference;
import io.github.sandydunlop.cascara.model.Text;
import io.github.sandydunlop.markista.orchestration.LinkResolver;
import io.github.sandydunlop.markista.orchestration.Relativizer;
import io.github.sandydunlop.markista.orchestration.TextAssembler;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.InvalidPathException;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

import jdk.javadoc.doclet.Reporter;

import com.sun.source.util.DocTreePath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PackageWriterTests {
    LinkResolver resolver;
    Api api;
    ModuleNode moduleNode;
    PackageNode packageNode;
    PackageNode modelPackage;

    Writer writer;

    PackageWriter packageWriter;

    private Context ctx;

    @AfterEach
    void resetSingleton() {
        Context.reset();
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
    void setup() {
        // Mocking Context interfered with other tests, so we're doing this the manual way
        writer = new StringWriter();
        ctx = Context.getInstance();
        ctx.setWriterFactory(_ -> writer);
        ctx.setReporter(reporter);
        packageWriter = new PackageWriter(api, ctx);

        api = new Api("Test API");

        moduleNode = new ModuleNode("markista");
        packageNode = new PackageNode("io.github.sandydunlop.markista");
        modelPackage = new PackageNode("io.github.sandydunlop.markista.model");

        Text body = Text.empty();
        body.append(Text.Segment.empty()
                .setKind(Text.Segment.Kind.TEXT)
                .setText("This is a test class [Node](Node)."));
        ClassNode nodeClass = new ClassNode(ModelUtil.createName(packageNode.getName()+".Node", packageNode.getName()));
        nodeClass.setFirstSentence(body);

        modelPackage.addType(nodeClass);
        packageNode.addPackage(modelPackage);

        PackageReference pr1 = new PackageReference(packageNode.getName());
        moduleNode.addPackage(pr1);
        PackageReference pr2 = new PackageReference(modelPackage.getName());
        moduleNode.addPackage(pr2);

        api.addModule(moduleNode);
        api.addPackage(packageNode);
        api.addPackage(modelPackage);
        api.addType(nodeClass);
		resolver = new LinkResolver(api, ctx);
		Relativizer.setFlattenedDirectories(null);
    }

    void setupLinks() {
        ctx = Context.getInstance();
        ctx.setApi(api);
        resolver = new LinkResolver(api, ctx);
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
        EnumNode enumNode = new EnumNode(ModelUtil.createName(modelPackage.getName()+".TestEnum", modelPackage.getName()));
        InterfaceNode interfaceNode = new InterfaceNode(ModelUtil.createName(modelPackage.getName()+".TestInterface", modelPackage.getName()));
        AnnotationNode annotationNode = new AnnotationNode(ModelUtil.createName(modelPackage.getName()+".TestAnnotation", modelPackage.getName()));
        modelPackage.addType(enumNode);
        modelPackage.addType(interfaceNode);
        modelPackage.addType(annotationNode);
        api.addType(enumNode);
        api.addType(interfaceNode);
        api.addType(annotationNode);
        setupLinks();
        ctx.setApi(api);

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
        EnumNode enumNode = new EnumNode(ModelUtil.createName(modelPackage.getName()+".TestEnum", modelPackage.getName()));
        InterfaceNode interfaceNode = new InterfaceNode(ModelUtil.createName(modelPackage.getName()+".TestInterface", modelPackage.getName()));
        AnnotationNode annotationNode = new AnnotationNode(ModelUtil.createName(modelPackage.getName()+".TestAnnotation", modelPackage.getName()));
        modelPackage.addType(enumNode);
        modelPackage.addType(interfaceNode);
        modelPackage.addType(annotationNode);
        api.addType(enumNode);
        api.addType(interfaceNode);
        api.addType(annotationNode);
        setupLinks();
        ctx.setApi(api);

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

