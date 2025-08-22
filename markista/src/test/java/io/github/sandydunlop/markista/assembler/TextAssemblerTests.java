package io.github.sandydunlop.markista.assembler;

import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassTypeNode;
import io.github.sandydunlop.markista.model.DirectiveNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.Pair;
import io.github.sandydunlop.markista.model.RecordTypeNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.Text.Segment;
import io.github.sandydunlop.markista.model.Text.SegmentKind;
import io.github.sandydunlop.markista.model.TypeNode;

import java.util.List;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

import jdk.javadoc.doclet.Reporter;

import com.sun.source.util.DocTreePath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextAssemblerTests {
    private static Context ctx;
	private Api api;
    private ModuleNode module;
    private PackageNode model;
    private PackageNode doclet;
	private PackageNode markista;
	private PackageNode util;
    private ClassTypeNode node;
    private ClassTypeNode markdownDoclet;

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
    }

    @BeforeEach
    void init() {
		api = new Api("Test API");
        api.addPackage(new PackageNode("io.github.sandydunlop"));
        markista = new PackageNode("io.github.sandydunlop.markista");
		util = new PackageNode("io.github.sandydunlop.markista.util");
		doclet = new PackageNode("io.github.sandydunlop.markista.doclet");
		model = new PackageNode("io.github.sandydunlop.markista.model");
		api.addPackage(markista);
		api.addPackage(util);
		api.addPackage(doclet);
		api.addPackage(model);
		api.addType(new ClassTypeNode("io.github.sandydunlop.markista.util.LinkResolver","LinkResolver", 
                util.getQualifiedName()));
		api.addType(new ClassTypeNode("io.github.sandydunlop.markista.doclet.MarkdownDoclet","MarkdownDoclet", 
                doclet.getQualifiedName()));
		api.addType(new ClassTypeNode("io.github.sandydunlop.markista.doclet.MarkdownDoclet.Option","MarkdownDoclet.Option", 
                doclet.getQualifiedName()));

        module = new ModuleNode("markista");
		api.addModule(module);
		module.addPackage(markista);
		module.addPackage(util);
		module.addPackage(doclet);
		module.addPackage(model);
		markista.setModuleName(module.getName());
		util.setModuleName(module.getName());
		doclet.setModuleName(module.getName());
		model.setModuleName(module.getName());

        node = new ClassTypeNode("io.github.sandydunlop.markista.model.Node", 
                "Node", model.getQualifiedName());
        model.addType(node);
        api.addType(node);

        markdownDoclet = new ClassTypeNode("io.github.sandydunlop.markista.doclet.MarkdownDoclet", 
                "MarkdownDoclet", doclet.getQualifiedName());
        model.addType(markdownDoclet);

        LinkResolver.init(api, ctx);
        LinkResolver.addNativeModules();
        TextAssembler.assembleTextAndLinks(api, ctx);
        LinkResolver.setFlattenedDirectories(null);
		ctx.setModuleName("markista");
        ctx.setPackageName("io.github.sandydunlop.markista.doclet");
    }

    static Segment text(String t) {
        return Segment.empty().setKind(SegmentKind.TEXT).setText(t);
    }

    static Segment link(String t, String u) {
        return Segment.empty().setKind(SegmentKind.LINK).setText(t).setLink(new Reference().withUri(u));
    }

    static List<Object[]> typeReferenceProvider() {
        return List.of(
            new Object[] { "java.lang.String[]", new Segment[] {
                    link("String", "https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/lang/String.html"), text("[]") } },
            new Object[] { "java.util.List<java.lang.String[]>", new Segment[] {
                    link("List", "https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/util/List.html"), 
                    text("<"), link("String", "https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/lang/String.html"), 
                    text("[]"), text(">")} },
            new Object[] {"java.util.function.Function<java.lang.String,java.util.Optional<java.lang.String>>", new Segment[] {
                    link("Function", "https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/util/function/Function.html"),
                    text("<"), link("String", "https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/lang/String.html"),
                    text(", "), link("Optional", "https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/util/Optional.html"),
                    text("<"), link("String", "https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/lang/String.html"),
                    text(">"), text(">")} }
        );
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("typeReferenceProvider")
	void link_to_text(String target, Segment[] expected) {
        Reference link = Reference.to(target);
        Text text = TextAssembler.link(link);
        assertEquals(expected.length, text.getSegments().size());
        for (int i=0; i<expected.length; i++) {
            Segment expectedSegment = expected[i];
            Segment actualSegment = text.getSegment(i);
            assertEquals(expectedSegment.getText(), actualSegment.getText());
            if (expectedSegment.getKind() == SegmentKind.LINK) {
                assertEquals(expectedSegment.getLink().getUri(), actualSegment.getLink().getUri());
            }
        }
    }

    @Test
    void processModules() {
        TextAssembler.processModules(api);
        ModuleNode module2 = api.getModules().getFirst();
        for (DirectiveNode directive : module2.getDirectives()) {
            assertNotEquals("", directive.getReference().getUri());
            assertNotEquals("", directive.getReference().getUri());

            for (Reference implementation : directive.getImplementations()) {
                assertNotEquals("", implementation.getUri());
            }
            for (Reference pkg : directive.getPackages()) {
                assertNotEquals("", pkg.getUri());
            }
        }

    }

    @Test
    void processModules_constantValues() {
        FieldNode constantValue = new FieldNode("java.lang.String", "fieldName");
        node.addField(constantValue);
        module.addConstantValue(constantValue);
        constantValue.setConstantValue("testValue");
        module.addConstantValue(constantValue);     

        TextAssembler.processModules(api);

        Reference constantReference = constantValue.getConstantValueReference();
        assertNotNull(constantReference);
        assertEquals(Reference.Kind.URL, constantReference.getKind());
        assertEquals("https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/lang/String.html", constantReference.getUri());
    }

    @Test
    void test_addJavadocToRecords() {
        RecordTypeNode recordNode = new RecordTypeNode("io.github.sandydunlop.markista.model.RecordTest","RecordTest",model.getQualifiedName());
        MethodNode equalsNode = new MethodNode("boolean","equals");
        MethodNode hashCode = new MethodNode("long","hashCode");
        MethodNode toString = new MethodNode("java.lang.String","toString");
        MethodNode test = new MethodNode("java.lang.String","test");
        recordNode.addMethod(equalsNode);
        recordNode.addMethod(hashCode);
        recordNode.addMethod(toString);
        recordNode.addMethod(test);

        api.addType(recordNode);
        TextAssembler.addJavadocToRecords(api);

        assertEquals("Indicates whether some other object is \"equal to\" this one.",
                equalsNode.getFirstSentence().toString());

        assertEquals("Returns a hash code value for this object.",
                hashCode.getFirstSentence().toString());

        assertEquals("Returns a string representation of this record class.",
                toString.getFirstSentence().toString());

        assertEquals("Returns the value of the `test` record component.",
                test.getFirstSentence().toString());
    }

    @Test
    void inheritance() {
        TypeNode baseTypeNode = new TypeNode("io.github.sandydunlop.markista.BaseType", 
                "BaseType", "io.github.sandydunlop.markista");
        MethodNode baseTypeMethod = new MethodNode("java.lang.String","toString");
        baseTypeMethod.setOwnerName(baseTypeNode.getQualifiedName());
        baseTypeNode.addMethod(baseTypeMethod);
        markista.addType(baseTypeNode);
        api.addType(baseTypeNode);

        TypeNode typeNode = new TypeNode("io.github.sandydunlop.markista.Type", 
                "BaseType", "io.github.sandydunlop.markista");
        MethodNode typeMethod = new MethodNode("java.lang.String","toString");
        typeMethod.setOwnerName(typeNode.getQualifiedName());
        typeNode.addMethod(typeMethod);
        markista.addType(typeNode);
        api.addType(typeNode);

        // Add some javadoc
        baseTypeMethod.setFirstSentence(Text.of("BaseDoc"));
        Text typeMethodText = Text.of(Segment.empty().setKind(SegmentKind.INHERIT));
        typeMethod.setFirstSentence(typeMethodText);

        // Set baseTypeNode as the supertype of typeNode
        Reference supertype = Reference.to(baseTypeNode.getQualifiedName());
        Pair<Reference,Text> pair = Pair.of(supertype, Text.empty());
        typeNode.getSupertypes().add(pair);
        typeMethod.setBaseMethod(pair);

        TextAssembler.assembleTextAndLinks(api, ctx);

        // Verify
        assertEquals("BaseDoc", typeMethod.getFirstSentence().toString());
    }

    @Test
    void moduleDirectives() {
        Reference ref1 = Reference.to("io.github.sandydunlop.markista");
        DirectiveNode directive1 = new DirectiveNode(DirectiveNode.Kind.EXPORTS, ref1);
        module.addDirective(directive1);

        TextAssembler.assembleTextAndLinks(api, ctx);

        assertTrue(ref1.isResolved());
    }
}
