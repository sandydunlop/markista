package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassTypeNode;
import io.github.sandydunlop.markista.model.EnumTypeNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.util.LinkFormatter;
import io.github.sandydunlop.markista.util.LinkResolver;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

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

        api = new Api("Test API");
        api.addModule(moduleNode);
        api.addPackage(packageNode);
        api.addPackage(modelPackage);
        api.addType(nodeClass);
		LinkResolver.init(api, contextMock);
		LinkResolver.setFlattenedDirectories(null);
    }

    @Test
    void blah() throws IOException {
        EnumTypeNode enumNode = new EnumTypeNode("io.github.sandydunlop.markista.model.TestEnum", "TestEnum", 
                packageNode.getQualifiedName());
        FieldNode constant1 = new FieldNode("io.github.sandydunlop.markista.model.TestEnum", "field1");
        constant1.setConstantValue("1");
        enumNode.addConstant(constant1);
        modelPackage.addType(enumNode);
        api.addType(enumNode);
        modelPackage.addType(enumNode);

        Context ctx = Context.getInstance();

        LinkResolver.init(api, ctx);
        LinkResolver.addNativeModules();
        LinkFormatter.generateLinkTexts(api, ctx);

        packageWriter.outputPackageDoc(packageNode);
        String output = writer.toString();
        assertTrue(output.contains("Packages"));
        assertTrue(output.contains("[model](model/index.md)"));
    }
}

