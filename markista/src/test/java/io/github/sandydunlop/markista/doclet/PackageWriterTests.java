package io.github.sandydunlop.markista.doclet;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

import com.sun.source.util.DocTreePath;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.EnumNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.util.Configuration;
import io.github.sandydunlop.markista.util.FileUtils;
import io.github.sandydunlop.markista.util.LinkResolver;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class PackageWriterTests {
    Api api;
    FileUtils files;
    ModuleNode moduleNode;
    PackageNode packageNode;
    PackageWriter packageWriter;

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
		Configuration.setReporter(reporter);
        Configuration.setOutputDirectory("/tmp/doc");
    }

    @BeforeEach
    void init() {
        files = new FileUtils(moduleNode, "/tmp/doc");

        moduleNode = new ModuleNode("markista");
        packageNode = new PackageNode("io.github.sandydunlop.markista");
        PackageNode modelPackage = new PackageNode("io.github.sandydunlop.markista.model");

        TypeNode returnType = new TypeNode("io.github.sandydunlop.markista.model.Node", "Node", packageNode);
        MethodNode methodNode = new MethodNode(returnType, "method");
        Text body = Text.empty();
        body.append(Text.Segment.empty()
                .setKind(Text.SegmentKind.MARKDOWN)
                .setText("This is a test class [Node](Node)."));
        methodNode.setBody(body);
        methodNode.setFullBody(body);
        methodNode.setFirstSentence(body);


        ClassNode nodeClass = new ClassNode("io.github.sandydunlop.markista.model.Node", "Node", packageNode);
        nodeClass.setBody(body);
        nodeClass.setFullBody(body);
        nodeClass.setFirstSentence(body);
        nodeClass.addMethod(methodNode);


        EnumNode enumNode = new EnumNode("io.github.sandydunlop.markista.model.TestEnum", "TestEnum", packageNode);
        TypeNode fieldType = new TypeNode("io.github.sandydunlop.markista.model.TestEnum", "TestEnum", packageNode);
        FieldNode constant1 = new FieldNode(fieldType, "field1");
        constant1.setConstantValue("1");
        enumNode.addConstant(constant1);

        modelPackage.addEnum(enumNode);
        modelPackage.addClass(nodeClass);
        packageNode.addPackage(modelPackage);
        moduleNode.addPackage(packageNode);
        moduleNode.addPackage(modelPackage);

        api = new Api();
        api.addModule(moduleNode);
        api.addPackage(packageNode);
        api.addPackage(modelPackage);
        api.addClass(nodeClass);
        api.addEnum(enumNode);
        String moduleDir = Configuration.getOutputDirectory() + "/" + moduleNode.getName();
		LinkResolver.init(api);
		LinkResolver.setFlattenedDirectories(null);
		LinkResolver.setCurrentModuleName("markista");
        LinkResolver.setCurrentPackageName("");
        packageWriter = new PackageWriter(moduleDir);
    }

    @Test
    void outputPackageDoc() throws IOException {
        packageWriter.writeDocs(moduleNode);
        Path filePath = Paths.get("/tmp/doc/markista/io/github/sandydunlop/markista/index.md");
        assertTrue(Files.exists(filePath));
    }
}

