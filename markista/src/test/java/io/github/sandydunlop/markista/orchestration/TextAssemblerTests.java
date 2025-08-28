package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.ModelTestEnvironment;
import io.github.sandydunlop.markista.markdown.MarkdownUtils;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.DirectiveNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.InterfaceNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.RecordNode;
import io.github.sandydunlop.markista.model.Link;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.Text.Segment;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.model.TypeReference;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextAssemblerTests extends ModelTestEnvironment {

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

    static Segment text(String t) {
        return Segment.empty().setKind(Segment.Kind.TEXT).setText(t);
    }

    static Segment link(String t, String u) {
        return Segment.empty().setKind(Segment.Kind.LINK).setText(t).setLink(new Link().withUri(u));
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

    //TODO: disabled while refactoring TextAssembler
    // @org.junit.jupiter.params.ParameterizedTest
    // @org.junit.jupiter.params.provider.MethodSource("typeReferenceProvider")
	// void link_to_text(String target, Segment[] expected) {
    //     Link link = Link.to(target);
    //     Text text = TextAssembler.link(link);
    //     assertEquals(expected.length, text.getSegments().size());
    //     for (int i=0; i<expected.length; i++) {
    //         Segment expectedSegment = expected[i];
    //         Segment actualSegment = text.getSegment(i);
    //         assertEquals(expectedSegment.getText(), actualSegment.getText());
    //         if (expectedSegment.getKind() == Segment.Kind.LINK) {
    //             assertEquals(expectedSegment.getLink().getUri(), actualSegment.getLink().getUri());
    //         }
    //     }
    // }

    @Test
    void processModules() {
        TextAssembler.processModules(api.getModules());
        for (DirectiveNode directive : module.getDirectives()) {
            assertNotEquals("", directive.getReference().getUri());
            assertNotEquals("", directive.getReference().getUri());

            for (Link implementation : directive.getImplementations()) {
                assertNotEquals("", implementation.getUri());
            }
            for (Link pkg : directive.getPackages()) {
                assertNotEquals("", pkg.getUri());
            }
        }
    }

    //TODO: disabled while refactoring TextAssembler
    @Disabled("WIP")
    @Test
    void processModules_constantValues() {
        FieldNode constantValue = new FieldNode("java.lang.String", "fieldName");
        node.addField(constantValue);
        module.addConstantValue(constantValue);
        constantValue.setConstantValue("testValue");
        module.addConstantValue(constantValue);

        TextAssembler.processModules(api.getModules());

        TypeReference constantReference = constantValue.getConstantValueReference();
        assertNotNull(constantReference);
        // assertEquals(Link.Kind.URL, constantReference.getKind());
        // assertEquals("https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/lang/String.html", constantReference.getUri());
    }

    @Test
    void test_addJavadocToRecords() {
        RecordNode recordNode = new RecordNode("RecordTest",model.getName());
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
        TypeNode baseTypeNode = new TypeNode("BaseType", "io.github.sandydunlop.markista");
        MethodNode baseTypeMethod = new MethodNode("java.lang.String","toString");
        baseTypeMethod.setOwnerName(baseTypeNode.getQualifiedName());
        baseTypeNode.addMethod(baseTypeMethod);
        markista.addType(baseTypeNode);
        api.addType(baseTypeNode);

        TypeNode typeNode = new TypeNode("MyType", "io.github.sandydunlop.markista");
        MethodNode typeMethod = new MethodNode("java.lang.String","toString");
        typeMethod.setOwnerName(typeNode.getQualifiedName());
        typeNode.addMethod(typeMethod);
        markista.addType(typeNode);
        api.addType(typeNode);

        // Add some javadoc
        baseTypeMethod.setFirstSentence(Text.of("BaseDoc"));
        Text typeMethodText = Text.of(Segment.empty().setKind(Segment.Kind.INHERIT));
        typeMethod.setFirstSentence(typeMethodText);

        // Set baseTypeNode as the supertype of typeNode
        TypeReference typeRef = TypeReference.to(baseTypeNode.getQualifiedName());
        typeNode.getSupertypes().add(typeRef);

        Link methodRef = Link.to(baseTypeNode.getQualifiedName() + "#" + baseTypeMethod.getSimpleName());
        typeMethod.setBaseMethod(methodRef);

        TextAssembler.assembleTextAndLinks(api, ctx);

        // Verify
        assertEquals("BaseDoc", typeMethod.getFirstSentence().toString());
    }

    @Test
    void moduleDirectives() {
        Link ref1 = Link.to("io.github.sandydunlop.markista");
        DirectiveNode directive1 = new DirectiveNode(DirectiveNode.Kind.EXPORTS, ref1);
        module.addDirective(directive1);

        TextAssembler.assembleTextAndLinks(api, ctx);

        assertTrue(ref1.isResolved());
        assertEquals("io/github/sandydunlop/markista", ref1.getUri());
    }

    @Test
    void inheritedMethods() {
        MethodNode test = new MethodNode("java.lang.String", "inheritedMethod");
        test.setOwnerName(node.getQualifiedName());
        node.addMethod(test);

        ClassNode subClass = newClass("SubClass", model);
        subClass.getSupertypes().add(TypeReference.to("java.lang.Object"));
        subClass.getSupertypes().add(TypeReference.to(node.getQualifiedName()));

        TextAssembler.assembleTextAndLinks(api, ctx);

        assertEquals(1, subClass.getInheritedMethods().size());
        Map.Entry<TypeReference, List<Link>> inheritedEntry = subClass.getInheritedMethods().entrySet().iterator().next();
        List<Link> inheritedMethods = inheritedEntry.getValue();
        assertEquals(1, inheritedMethods.size());
        Link inheritedMethod = inheritedMethods.getFirst();
        assertEquals("inheritedMethod", inheritedMethod.getMethodName());
    }

    //TODO: disabled while refactoring TextAssembler
	// @Test
	// void splitAndLink_oneArray_simplified() {
    //     Text text = TextAssembler.splitAndLink("java.lang.String[]");
    //     String markdown = MarkdownUtils.formatText(text);
    //     assertEquals("[String](" + JAVA_24_URL + "java.base/java/lang/String.html)[]", markdown);
    // }

    // @Test
    // void link_generics_qualified() {
    //     Text text = TextAssembler.linkGenerics("java.util.function.Function<java.lang.String,java.util.Optional<java.lang.String>>");
    //     String markdown = MarkdownUtils.formatText(text);
    //     assertEquals("[Function](https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/util/function/Function.html)<[String](https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/lang/String.html), [Optional](https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/util/Optional.html)<[String](https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/lang/String.html)>>", markdown);
    // }

    @Test
    void link_standardMethod() {
        LinkResolver.addStandardModules();
        Text text = TextAssembler.link(Link.to("jdk.javadoc.doclet.Doclet.Option#process(java.lang.String,java.util.List)"));
        String markdown = MarkdownUtils.formatText(text);
        assertEquals("[Doclet.Option.process(java.lang.String,java.util.List)](https://docs.oracle.com/en/java/javase/24/docs/api/jdk.javadoc/jdk/javadoc/doclet/Doclet.Option.html#process(java.lang.String,java.util.List))", markdown);
    }

    @Test
    void link_localMethod() {
        LinkResolver.addStandardModules();
        Text text = TextAssembler.link(Link.to("Node#sort()"));
        String markdown = MarkdownUtils.formatText(text);
        assertEquals("[Node.sort](../model/Node.md#sort)", markdown);
    }

    //TODO: disabled while refactoring TextAssembler
	// @Test
	// void splitAndLink_two() {
    //     Text text = TextAssembler.splitAndLink("java.lang.String, java.util.List");
    //     String markdown = MarkdownUtils.formatText(text);
    //     assertEquals("[String](" + JAVA_24_URL + "java.base/java/lang/String.html), [List](" + JAVA_24_URL + "java.base/java/util/List.html)", markdown);
    // }

    @Test
    void getStandardInterface_normal() {
        TypeReference interfaceRef = TypeReference.to("jdk.javadoc.doclet.Doclet");
        interfaceRef.getLink().setPackageName("jdk.javadoc.doclet");
        interfaceRef.getLink().setSimpleClassName("Doclet");
        InterfaceNode interfaceNode = TextAssembler.getStandardInterface(interfaceRef);
        assertNotNull(interfaceNode);
    }

    @Disabled("This used to work. What's wrong with it now?")
    @Test
    void getStandardInterface_nested() {
        TypeReference interfaceRef = TypeReference.to("jdk.javadoc.doclet.Doclet.Option");
        interfaceRef.getLink().setPackageName("jdk.javadoc.doclet");
        interfaceRef.getLink().setSimpleClassName("Doclet.Option");
        InterfaceNode interfaceNode = TextAssembler.getStandardInterface(interfaceRef);
        assertNotNull(interfaceNode);
    }

    @Test
    void getStandardInterface_nested2() {
        TypeReference interfaceRef = TypeReference.to("java.lang.Runnable");
        interfaceRef.getLink().setPackageName("java.lang");
        interfaceRef.getLink().setSimpleClassName("Runnable");
        InterfaceNode interfaceNode = TextAssembler.getStandardInterface(interfaceRef);
        assertNotNull(interfaceNode);
    }
}
