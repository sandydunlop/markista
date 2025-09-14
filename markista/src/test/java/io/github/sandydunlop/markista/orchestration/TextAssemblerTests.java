package io.github.sandydunlop.markista.orchestration;

import io.github.sandydunlop.markista.ModelTestEnvironment;
import io.github.sandydunlop.markista.markdown.MarkdownUtils;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.DirectiveNode;
import io.github.sandydunlop.markista.model.InterfaceNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.MethodReference;
import io.github.sandydunlop.markista.model.Name;
import io.github.sandydunlop.markista.model.RecordNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.Link;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.Text.Segment;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.model.VariableType;

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
            for (Link pkg : directive.getPackages()) {
                assertNotEquals("", pkg.getUri().toString());
            }
        }
    }

    @Test
    void test_addJavadocToRecords() {
        RecordNode recordNode = newRecord("RecordTest",model);
        Name equalsNodeName = new Name("equals", recordNode.getName().fullyQualifiedName(), recordNode.getPackageName());
        MethodNode equalsNode = new MethodNode("boolean",equalsNodeName);
        Name hashCodeNodeName = new Name("hashCode", recordNode.getName().fullyQualifiedName(), recordNode.getPackageName());
        MethodNode hashCode = new MethodNode("long",hashCodeNodeName);
        Name toStringNodeName = new Name("toString", recordNode.getName().fullyQualifiedName(), recordNode.getPackageName());
        MethodNode toString = new MethodNode("java.lang.String",toStringNodeName);
        Name testNodeName = new Name("test", recordNode.getName().fullyQualifiedName(), recordNode.getPackageName());
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
        Name baseMethodName = new Name("toString", baseTypeNode.getName().fullyQualifiedName(), baseTypeNode.getPackageName());
        MethodNode baseTypeMethod = new MethodNode("java.lang.String",baseMethodName);
        baseTypeMethod.setOwnerName(baseTypeNode.getName());
        baseTypeNode.addMethod(baseTypeMethod);
        markista.addType(baseTypeNode);
        api.addType(baseTypeNode);

        TypeNode typeNode = newClass("MyType", markista);
        Name methodName = new Name("toString", typeNode.getName().fullyQualifiedName(), typeNode.getPackageName());
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
        VariableType typeRef = VariableType.parse(baseTypeNode.getName().fullyQualifiedName());
        typeNode.getSupertypes().add(typeRef);

        Link methodRef = Link.to(new Reference(baseTypeNode.getName().fullyQualifiedName() + "#" + baseTypeMethod.getName().simpleName()));
        typeMethod.setBaseMethod(methodRef);

        TextAssembler.assembleTextAndLinks(api, ctx);

        // Verify
        assertEquals("BaseDoc", typeMethod.getFirstSentence().toString());
    }

    @Test
    void moduleDirectives() {
        Link link = Link.to(new Reference("io.github.sandydunlop.markista"));
        DirectiveNode directive1 = new DirectiveNode(DirectiveNode.Kind.EXPORTS, link);
        module.addDirective(directive1);

        ctx.setPackageName("");
        TextAssembler.assembleTextAndLinks(api, ctx);

        assertTrue(link.isResolved());
        assertEquals("io/github/sandydunlop/markista/", link.getUri().toString());
    }

    @Disabled("MFLP-107 Link doesnt contain method name")
    @Test
    void inheritedMethods() {
        Name methodName = new Name("inheritedMethod", node.getName().fullyQualifiedName(), node.getPackageName());
        MethodNode test = new MethodNode("java.lang.String", methodName);
        test.setOwnerName(node.getName());
        node.addMethod(test);

        ClassNode subClass = newClass("SubClass", model);
        subClass.getSupertypes().add(VariableType.parse("java.lang.Object"));
        subClass.getSupertypes().add(VariableType.parse(node.getName().fullyQualifiedName()));

        TextAssembler.assembleTextAndLinks(api, ctx);

        assertEquals(1, subClass.getInheritedMethods().size());
        Map.Entry<VariableType, List<MethodReference>> inheritedEntry = subClass.getInheritedMethods().entrySet().iterator().next();
        List<MethodReference> inheritedMethods = inheritedEntry.getValue();
        assertEquals(1, inheritedMethods.size());
    }

    @Disabled("resolveLink is being deprecated")
    @Test
    void link_standardMethod() {
        Link link = Link.to(new Reference("jdk.javadoc.doclet.Doclet.Option#process(java.lang.String,java.util.List)"));
        resolver.resolveLink(link);
        String markdown = MarkdownUtils.link(link, true);
        assertEquals("[Doclet.Option.process](https://docs.oracle.com/en/java/javase/24/docs/api/jdk.javadoc/jdk/javadoc/doclet/Doclet.Option.html#process(java.lang.String,java.util.List))", markdown);
    }

    @Disabled("resolveLink is being deprecated")
    @Test
    void link_localMethod() {
        Link link = Link.to(new Reference("Node#sort"));
        resolver.resolveLink(link);
        String markdown = MarkdownUtils.link(link, false);
        assertEquals("[Node.sort](../model/Node.md#sort)", markdown);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("standardInterfaceProvider")
    void getStandardInterface_parameterized(String qualifiedName, String packageName, String simpleClassName) {
        VariableType interfaceRef = VariableType.parse(qualifiedName);
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

    @Test
    void t1() {
        Reference ref = new Reference("jdk.javadoc.doclet.Doclet.Option.Kind");
        resolver.qualify(ref);
        assertTrue(ref.getName().isType());
    }
}
