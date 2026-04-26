package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.ModelTestEnvironment;
import io.github.sandydunlop.markista.markdown.MarkdownUtils;
import io.github.qishr.cascara.lang.java.model.ClassNode;
import io.github.qishr.cascara.lang.java.model.DirectiveNode;
import io.github.qishr.cascara.lang.java.model.InterfaceNode;
import io.github.qishr.cascara.lang.java.model.MethodNode;
import io.github.qishr.cascara.lang.java.model.MethodReference;
import io.github.qishr.cascara.lang.java.model.ModelUtil;
import io.github.qishr.cascara.lang.java.model.NameUtil;
import io.github.qishr.cascara.lang.java.model.JlsName;
import io.github.qishr.cascara.lang.java.model.RecordNode;
import io.github.qishr.cascara.lang.java.model.Reference;
import io.github.qishr.cascara.lang.java.model.Link;
import io.github.qishr.cascara.lang.java.model.Text;
import io.github.qishr.cascara.lang.java.model.Text.Segment;
import io.github.qishr.cascara.lang.java.model.TypeNode;
import io.github.qishr.cascara.lang.java.model.VariableTypeNode;

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
        resolver = new LinkResolver(api, ctx);
        TextAssembler.assembleTextAndLinks(api, ctx);
        Relativizer.setFlattenedDirectories(null);
		ctx.setModuleName("markista");
        ctx.setPackageName("io.github.sandydunlop.markista.doclet");
    }

    @Test
    void processModules() {
        TextAssembler.processModules(api.getModules());
        for (DirectiveNode directive : module.getDirectives()) {
            assertNotEquals("", directive.getLink().getUri().toString());
            assertNotEquals("", directive.getLink().getUri().toString());

            for (Link implementation : directive.getImplementations()) {
                assertNotEquals("", implementation.getUri().toString());
            }

            // for (Link pkg : directive.getPackages()) {
            //     assertNotEquals("", pkg.getUri().toString());
            // }
        }
    }

    @Test
    void test_addJavadocToRecords() {
        RecordNode recordNode = newRecord("RecordTest",model);
        JlsName equalsNodeName = NameUtil.createMemberName(recordNode.getName(), "equals");
        MethodNode equalsNode = new MethodNode("boolean",equalsNodeName);
        JlsName hashCodeNodeName = NameUtil.createMemberName(recordNode.getName(), "hashCode");
        MethodNode hashCode = new MethodNode("long",hashCodeNodeName);
        JlsName toStringNodeName = NameUtil.createMemberName(recordNode.getName(), "toString");
        MethodNode toString = new MethodNode("java.lang.String",toStringNodeName);
        JlsName testNodeName = NameUtil.createMemberName(recordNode.getName(), "test");
        MethodNode test = new MethodNode("java.lang.String",testNodeName);
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
        TypeNode baseTypeNode = newClass("BaseType", markista);
        JlsName baseMethodName = NameUtil.createMemberName(baseTypeNode.getName(), "toString");
        MethodNode baseTypeMethod = new MethodNode("java.lang.String",baseMethodName);
        baseTypeMethod.setOwnerName(baseTypeNode.getName());
        baseTypeNode.addMethod(baseTypeMethod);
        markista.addType(baseTypeNode);
        api.addType(baseTypeNode);

        TypeNode typeNode = newClass("MyType", markista);
        JlsName methodName = NameUtil.createMemberName(typeNode.getName(), "toString");
        MethodNode typeMethod = new MethodNode("java.lang.String",methodName);
        typeMethod.setOwnerName(typeNode.getName());
        typeNode.addMethod(typeMethod);
        markista.addType(typeNode);
        api.addType(typeNode);

        // Add some javadoc
        baseTypeMethod.setFirstSentence(Text.of("BaseDoc"));
        Text typeMethodText = Text.of(Segment.empty().setKind(Segment.Kind.INHERIT));
        typeMethod.setFirstSentence(typeMethodText);

        // Set baseTypeNode as the supertype of typeNode
        VariableTypeNode typeRef = ModelUtil.parseVariableType(baseTypeNode.getName().fullyQualifiedName());
        typeNode.getSupertypes().add(typeRef);

        Link methodRef = Link.to(NameUtil.createReference(baseTypeNode.getName().fullyQualifiedName() + "#" + baseTypeMethod.getName().simpleName()));
        typeMethod.setBaseMethod(methodRef);

        TextAssembler.assembleTextAndLinks(api, ctx);

        // Verify
        assertEquals("BaseDoc", typeMethod.getFirstSentence().toString());
    }

    @Disabled("concurrency issues")
    @Test
    void moduleDirectives() {
        Link link = Link.to(NameUtil.createReference("io.github.sandydunlop.markista.model"));
        DirectiveNode directive1 = new DirectiveNode(DirectiveNode.Kind.EXPORTS, link);
        module.addDirective(directive1);

        ctx.setPackageName("");
        TextAssembler.assembleTextAndLinks(api, ctx);

        assertTrue(link.isResolved());
        assertEquals("io/github/sandydunlop/markista/model/", link.getUri().toString());
    }

    @Disabled("MFLP-107 Link doesnt contain method name")
    @Test
    void inheritedMethods() {
        JlsName methodName = NameUtil.createMemberName("inheritedMethod");
        MethodNode test = new MethodNode("java.lang.String", methodName);
        test.setOwnerName(node.getName());
        node.addMethod(test);

        ClassNode subClass = newClass("SubClass", model);
        subClass.getSupertypes().add(ModelUtil.parseVariableType("java.lang.Object"));
        subClass.getSupertypes().add(ModelUtil.parseVariableType(node.getName().fullyQualifiedName()));

        TextAssembler.assembleTextAndLinks(api, ctx);

        assertEquals(1, subClass.getInheritedMethods().size());
        Map.Entry<VariableTypeNode, List<MethodReference>> inheritedEntry = subClass.getInheritedMethods().entrySet().iterator().next();
        List<MethodReference> inheritedMethods = inheritedEntry.getValue();
        assertEquals(1, inheritedMethods.size());
    }

    @Disabled("resolveLink is being deprecated")
    @Test
    void link_standardMethod() {
        Link link = Link.to(NameUtil.createReference("jdk.javadoc.doclet.Doclet.Option#process(java.lang.String,java.util.List)"));
        resolver.resolveLink(link);
        String markdown = MarkdownUtils.link(link, true);
        assertEquals("[Doclet.Option.process](https://docs.oracle.com/en/java/javase/24/docs/api/jdk.javadoc/jdk/javadoc/doclet/Doclet.Option.html#process(java.lang.String,java.util.List))", markdown);
    }

    @Disabled("resolveLink is being deprecated")
    @Test
    void link_localMethod() {
        Link link = Link.to(NameUtil.createReference("Node#sort"));
        resolver.resolveLink(link);
        String markdown = MarkdownUtils.link(link, false);
        assertEquals("[Node.sort](../model/Node.md#sort)", markdown);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("standardInterfaceProvider")
    void getStandardInterface_parameterized(String qualifiedName, String packageName, String simpleClassName) {
        VariableTypeNode interfaceRef = ModelUtil.parseVariableType(qualifiedName);
        InterfaceNode interfaceNode = TextAssembler.getStandardInterface(interfaceRef);
        assertNotNull(interfaceNode);
    }

    private static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> standardInterfaceProvider() {
        return java.util.stream.Stream.of(
            org.junit.jupiter.params.provider.Arguments.of(
                "jdk.javadoc.doclet.Doclet", "jdk.javadoc.doclet", "Doclet"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "jdk.javadoc.doclet.Doclet.Option", "jdk.javadoc.doclet", "Doclet.Option"
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "java.lang.Runnable", "java.lang", "Runnable"
            )
        );
    }

}
