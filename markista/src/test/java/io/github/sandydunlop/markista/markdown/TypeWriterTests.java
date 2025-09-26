package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.ModelTestEnvironment;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.cascara.model.AnnotationElement;
import io.github.sandydunlop.cascara.model.AnnotationNode;
import io.github.sandydunlop.cascara.model.Api;
import io.github.sandydunlop.cascara.model.AppliedAnnotationNode;
import io.github.sandydunlop.cascara.model.ClassNode;
import io.github.sandydunlop.cascara.model.Deprecation;
import io.github.sandydunlop.cascara.model.EnumNode;
import io.github.sandydunlop.cascara.model.FieldNode;
import io.github.sandydunlop.cascara.model.InterfaceNode;
import io.github.sandydunlop.cascara.model.MethodNode;
import io.github.sandydunlop.cascara.model.MethodReference;
import io.github.sandydunlop.cascara.model.Modifier;
import io.github.sandydunlop.cascara.model.Name;
import io.github.sandydunlop.cascara.model.PackageNode;
import io.github.sandydunlop.cascara.model.ParamNode;
import io.github.sandydunlop.cascara.model.RecordNode;
import io.github.sandydunlop.cascara.model.Reference;
import io.github.sandydunlop.cascara.model.Link;
import io.github.sandydunlop.cascara.model.Text;
import io.github.sandydunlop.cascara.model.TypeNode;
import io.github.sandydunlop.cascara.model.VariableType;
import io.github.sandydunlop.cascara.model.ModelUtil;
import io.github.sandydunlop.markista.orchestration.LinkResolver;
import io.github.sandydunlop.markista.orchestration.TextAssembler;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeWriterTests extends ModelTestEnvironment {
    private Writer writer;
    private TypeWriter typeWriter;

    @BeforeEach
    void setup() {
        setupModel();
        writer = new StringWriter();
        ctx.setWriterFactory(_ -> writer);
        typeWriter = new TypeWriter(ctx);
    }

    @Test
    void writeDoc_CreatesDocForSimpleClass_TypeNameSetAndReset() throws IOException {
        TypeNode typeNode = newClass("MyClass", model);
        api.addType(typeNode);

        typeWriter.outputTypeDoc(typeNode);

        String content = writer.toString();
        assertTrue(content.contains("# Class MyClass"));
        assertTrue(content.contains("Package [io.github.sandydunlop.markista.model](index.md)"));
    }

    @Test
    void outputFieldSummary_WritesMarkdownTableForFields() throws IOException {
        FieldNode field1 = new FieldNode("java.lang.String", "fieldOne");
        field1.addModifier(Modifier.PUBLIC);
        FieldNode field2 = new FieldNode("int", "fieldTwo");
        field2.addModifier(Modifier.PRIVATE);

        TypeNode type = newClass("MyClass", model);
        type.addField(field1);
        type.addField(field2);
        api.addType(type);

        resolver = new LinkResolver(api, ctx);
        TextAssembler.assembleTextAndLinks(api, ctx);

        typeWriter.outputTypeDoc(type);

        String output = writer.toString();
        assertTrue(output.contains("## Field Summary"));
        assertTrue(output.contains("public [String]"));
        assertTrue(output.contains("private int"));
        assertTrue(output.contains("fieldOne"));
        assertTrue(output.contains("fieldTwo"));
    }

    @Test
    void outputEnumConstantsSummary_WritesEnumConstantsTable() throws IOException {
        EnumNode enumNode = newEnum("MyEnum", model);
        Link ref = Link.toWeb(URI.create("http://example.com"));
        List<Link> references = List.of(ref);
        FieldNode constant1 = newField("int", "CONST_ONE", node);
        constant1.setFirstSentence(Text.of("Constant one desc"));
        constant1.setSince(Text.of("Constant one since"));
        constant1.setReferences(references);
        FieldNode constant2 = newField("int", "CONST_TWO", node);
        constant2.setFirstSentence(Text.of("Constant two desc"));
        constant2.setSince(Text.of("Constant two since"));
        constant2.setReferences(references);
        enumNode.addConstant(constant1);
        enumNode.addConstant(constant2);

        resolver = new LinkResolver(api, ctx);
        TextAssembler.assembleTextAndLinks(api, ctx);
        MarkdownUtils.setContext(ctx);

        typeWriter.outputTypeDoc(enumNode);

        String output = writer.toString();
        assertTrue(output.contains("##Enum Constants"));
        assertTrue(output.contains("CONST_ONE"));
        assertTrue(output.contains("Constant one desc"));
        assertTrue(output.contains("CONST_TWO"));
        assertTrue(output.contains("Constant two desc"));
    }

    @Test
    void outputEnumConstantDetails_IncludesSinceAndReferences() throws IOException {
        EnumNode enumNode = newEnum("MyEnum", model);

        FieldNode fieldNode = newField("int", "CONST", node);
        fieldNode.setFirstSentence(Text.of("Full body text for const"));
        fieldNode.setSince(Text.of("Since version 1.0"));

        Link ref = Link.to(new Reference("io.github.sandydunlop.cascara.model.Node"))
                .withKind(Link.Kind.TYPE);
        fieldNode.getReferences().add(ref);

        enumNode.addConstant(fieldNode);

        resolver = new LinkResolver(api, ctx);
        TextAssembler.assembleTextAndLinks(api, ctx);

        typeWriter.outputTypeDoc(enumNode);

        String output = writer.toString();

        assertTrue(output.contains("### CONST"));
        assertTrue(output.contains("Full body text for const"));
        assertTrue(output.contains("Since version 1.0"));
    }

    @Test
    void outputDeclaration() throws IOException {
        PackageNode pkg = new PackageNode("scenario.food.category");
        AnnotationNode typeNode = new AnnotationNode(ModelUtil.createName(pkg.getName()+".SaladIngredient", pkg.getName()));

        AppliedAnnotationNode appliedAnnotation = new AppliedAnnotationNode("scenario.food.category.Target", "scenario.food.category");
        AnnotationElement param = new AnnotationElement(null, "value", ElementType.TYPE.toString());
        appliedAnnotation.addElement(param);

        ctx.setApi(new Api(""));
        typeNode.addAppliedAnnotation(appliedAnnotation);

        typeWriter.outputTypeDoc(typeNode);
        String output = writer.toString();
        assertTrue(output.contains("@interface __SaladIngredient__"));
    }

    @Test
    void outputDeprecation() throws IOException {
        PackageNode pkg = new PackageNode("scenario.food.berry");
        ClassNode typeNode = newClass("Avocado", pkg);

        Name methodName = ModelUtil.createName("eat", typeNode.getName().fullyQualifiedName(), pkg.getName());
        MethodNode methodNode = new MethodNode("java.lang.String", methodName);

        methodNode.setDeprecation(Deprecation.DEPRECATED);
        methodNode.setDeprecationText(Text.empty().append("This is deprecated"));
        typeNode.addMethod(methodNode);
        ctx.setApi(new Api(""));

        typeWriter.outputTypeDoc(typeNode);
        String output = writer.toString();
        assertTrue(output.contains("This is deprecated"));
    }

    @Test
    void outputDeprecation_noText() throws IOException {
        PackageNode pkg = new PackageNode("scenario.food.berry");
        ClassNode typeNode = newClass("Avocado", pkg);

        Name methodName = ModelUtil.createName("eat", typeNode.getName().fullyQualifiedName(), pkg.getName());
        MethodNode methodNode = new MethodNode("java.lang.String", methodName);

        methodNode.setDeprecation(Deprecation.DEPRECATED);
        typeNode.addMethod(methodNode);

        ctx.setPackageName("scenario.food.berry");
        Api api = new Api("Test API");
        api.addPackage(pkg);
        api.addType(typeNode);
        pkg.addType(typeNode);
        resolver = new LinkResolver(api, ctx);
        ctx.setApi(api);

        typeWriter.outputTypeDoc(typeNode);
        String output = writer.toString();
        assertTrue(output.contains("This has been marked as deprecated"));
    }

    @Test
    void outputMethodParams() throws IOException {
        Context.reset();
        PackageNode pkg = new PackageNode("scenario.food.berry");
        ClassNode typeNode = newClass("Avocado", pkg);
        Name methodName = ModelUtil.createName("eat", typeNode.getName().fullyQualifiedName(), pkg.getName());
        MethodNode methodNode = new MethodNode("java.lang.String", methodName);

        ParamNode param1 = new ParamNode("java.lang.String", "param1");
        param1.setFirstSentence(Text.empty().append("param1doc"));
        methodNode.addParam(param1);
        ParamNode param2 = new ParamNode("java.lang.String", "param2");
        param2.setFirstSentence(Text.empty().append("param2doc"));
        methodNode.addParam(param2);

        typeNode.addMethod(methodNode);

        ctx.setPackageName("scenario.food.berry");
        Api api = new Api("Test API");
        api.addPackage(pkg);
        api.addType(typeNode);
        pkg.addType(typeNode);
        resolver = new LinkResolver(api, ctx);
        TextAssembler.assembleTextAndLinks(api, ctx);
        ctx.setApi(api);

        typeWriter.outputTypeDoc(typeNode);
        String output = writer.toString();
        assertTrue(output.contains("[eat](#eat)([String](https"));
        assertTrue(output.contains("param2doc"));
    }

    @Test
    void outputMethodDetails() throws IOException {
        PackageNode pkg = new PackageNode("scenario.food.berry");
        ClassNode typeNode = newClass("Avocado", pkg);
        Name methodName = ModelUtil.createName("eat", typeNode.getName().fullyQualifiedName(), pkg.getName());
        MethodNode methodNode = new MethodNode("java.lang.String", methodName);
        methodNode.setOwnerName(typeNode.getName());

        Name overriddenMethodName = ModelUtil.createName("eat", typeNode.getName().fullyQualifiedName(), pkg.getName());
        MethodNode overriddenMethodNode = new MethodNode("java.lang.String", overriddenMethodName);
        overriddenMethodNode.setOwnerName(typeNode.getName());

        Link thrownRef = Link.to(new Reference("scenario.food.berry.Thrown"))
                .withKind(Link.Kind.TYPE);
        Link specifiedByRef = Link.to(new Reference("scenario.food.berry.Specified"))
                .withKind(Link.Kind.TYPE);

        InterfaceNode specifiedByType = newInterface("Specified", pkg);
        specifiedByType.addMethod(overriddenMethodNode);
        VariableType interfaceRef = VariableType.parse(specifiedByType.getName().fullyQualifiedName());
        typeNode.getImplementedInterfaces().add(interfaceRef);

        ClassNode thrownType = newClass("Thrown", pkg);

        methodNode.setReturnDescription(Text.empty().append("returnDescription"));
        methodNode.setSpecifiedBy(specifiedByRef);
        methodNode.addThrownType(thrownRef);
        Link overriddenMethod = Link.to(new Reference("scenario.food.berry.Avocado" + "#" + "eat"));
        methodNode.setBaseMethod(overriddenMethod);

        typeNode.addMethod(methodNode);

        ctx.setPackageName("scenario.food.berry");
        Api api = new Api("Test API");
        api.addPackage(pkg);
        api.addType(typeNode);
        pkg.addType(typeNode);
        api.addType(specifiedByType);
        api.addType(thrownType);
        api.addMethod(overriddenMethodNode);
        api.addMethod(methodNode);
        resolver = new LinkResolver(api, ctx);
        ctx.setApi(api);

        TextAssembler.assembleTextAndLinks(api, ctx);

        typeWriter.outputTypeDoc(typeNode);
        String output = writer.toString();
        assertTrue(output.contains("returnDescription"));
        assertTrue(output.contains("Specified.md"));
        assertTrue(output.contains("Thrown.md"));
        assertTrue(output.contains("eat"));
    }

    @Test
    void outputMethodDetails_Since() throws IOException {
        PackageNode pkg = new PackageNode("scenario.food.berry");
        ClassNode typeNode = newClass("Avocado",pkg);
        Name methodName = ModelUtil.createName("eat", typeNode.getName().fullyQualifiedName(), pkg.getName());
        MethodNode methodNode = new MethodNode("java.lang.String", methodName);

        Text since = Text.of("1980");
        methodNode.setSince(since);

        typeNode.addMethod(methodNode);

        ctx.setPackageName("scenario.food.berry");
        Api api = new Api("Test API");
        api.addPackage(pkg);
        api.addType(typeNode);
        resolver = new LinkResolver(api, ctx);
        ctx.setApi(api);

        TextAssembler.assembleTextAndLinks(api, ctx);

        typeWriter.outputTypeDoc(typeNode);
        String output = writer.toString();
        assertTrue(output.contains("Since"));
        assertTrue(output.contains("1980"));
    }

    @Test
    void outputMethodDetails_References() throws IOException {
        PackageNode pkg = new PackageNode("scenario.food.berry");
        ClassNode typeNode = newClass("Avocado", pkg);
        Name methodName = ModelUtil.createName("eat", typeNode.getName().fullyQualifiedName(), pkg.getName());
        MethodNode methodNode = new MethodNode("java.lang.String", methodName);

        Link ref = Link.toWeb(URI.create("http://example.com"));
        methodNode.getReferences().add(ref);

        typeNode.addMethod(methodNode);

        ctx.setPackageName("scenario.food.berry");
        Api api = new Api("Test API");
        api.addPackage(pkg);
        api.addType(typeNode);
        ctx.setApi(api);

        resolver = new LinkResolver(api, ctx);
        TextAssembler.assembleTextAndLinks(api, ctx);

        typeWriter.outputTypeDoc(typeNode);
        String output = writer.toString();
        assertTrue(output.contains("See Also"));
        assertTrue(output.contains("example.com"));
    }

    @Test
    void outputConstructorSummary() throws IOException {
        PackageNode pkg = new PackageNode("scenario.food.berry");
        ClassNode typeNode = newClass("Avocado", pkg);
        Name methodName = ModelUtil.createName("Avocado", typeNode.getName().fullyQualifiedName(), pkg.getName());
        MethodNode methodNode = new MethodNode("scenario.food.berry.Avocado", methodName);
        typeNode.addConstructor(methodNode);

        ctx.setPackageName("scenario.food.berry");
        Api api = new Api("Test API");
        api.addPackage(pkg);
        api.addType(typeNode);
        pkg.addType(typeNode);
        resolver = new LinkResolver(api, ctx);
        TextAssembler.assembleTextAndLinks(api, ctx);
        ctx.setApi(api);

        typeWriter.outputTypeDoc(typeNode);
        String output = writer.toString();
        assertTrue(output.contains("## Constructor Summary"));
        assertTrue(output.contains("Avocado()"));
    }

    @Test
    void outputImplementedInterfaces() throws IOException {
        PackageNode pkg = new PackageNode("scenario.food.berry");
        ClassNode typeNode = newClass("Avocado", pkg);
        typeNode.getImplementedInterfaces().add(VariableType.parse("interface"));

        InterfaceNode testInterface = newInterface("interface", pkg);

        ctx.setPackageName("scenario.food.berry");
        Api api = new Api("Test API");
        api.addPackage(pkg);
        api.addType(typeNode);
        pkg.addType(typeNode);
        api.addType(testInterface);
        pkg.addType(testInterface);
        resolver = new LinkResolver(api, ctx);
        TextAssembler.assembleTextAndLinks(api, ctx);
        ctx.setApi(api);

        typeWriter.outputTypeDoc(typeNode);
        String output = writer.toString();
        assertTrue(output.contains("Implemented Interfaces"));
        assertTrue(output.contains("[scenario.food.berry.interface]"));
    }

    @Test
    void outputEnclosedTypes() throws IOException {
        PackageNode pkg = new PackageNode("scenario.food.berry");
        ClassNode typeNode = newClass("Tomato", pkg);

        ClassNode enclosedClass = newClass("Tomato.Red", pkg);
        EnumNode enclosedEnum = newEnum("Tomato.Orange", pkg);
        InterfaceNode enclosedInterface = newInterface("Tomato.Yellow",pkg);
        AnnotationNode enclosedAnnotation = newAnnotation("Tomato.Green", pkg);

        typeNode.addType(enclosedClass);
        typeNode.addType(enclosedEnum);
        typeNode.addType(enclosedInterface);
        typeNode.addType(enclosedAnnotation);

        ctx.setPackageName("scenario.food.berry");
        Api api = new Api("Test API");
        api.addPackage(pkg);
        api.addType(typeNode);
        pkg.addType(typeNode);
        resolver = new LinkResolver(api, ctx);
        TextAssembler.assembleTextAndLinks(api, ctx);
        ctx.setApi(api);

        typeWriter.outputTypeDoc(typeNode);
        String output = writer.toString();
        assertTrue(output.contains("Red"));
        assertTrue(output.contains("Orange"));
        assertTrue(output.contains("Yellow"));
        assertTrue(output.contains("Green"));
    }

    @Test
    void outputSupertypes() throws IOException {
        PackageNode pkg = new PackageNode("scenario.food.berry");
        ClassNode typeNode = newClass("Avocado", pkg);
        typeNode.getSupertypes().add(VariableType.parse("test.interface"));

        ctx.setPackageName("scenario.food.berry");
        Api api = new Api("Test API");
        api.addPackage(pkg);
        api.addType(typeNode);
        pkg.addType(typeNode);
        resolver = new LinkResolver(api, ctx);
        TextAssembler.assembleTextAndLinks(api, ctx);
        ctx.setApi(api);

        typeWriter.outputTypeDoc(typeNode);
        String output = writer.toString();
        assertTrue(output.contains("test.interface"));
        assertTrue(output.contains("test.interface"));
    }

    @Test
    void outputEnclosingClass() throws IOException {
        PackageNode pkg = new PackageNode("scenario.food.berry");
        ClassNode typeNode = new ClassNode(ModelUtil.createName("scenario.food.berry.Avocado", pkg.getName()));

        ClassNode owner = new ClassNode(ModelUtil.createName("scenario.food.berry.Owner", pkg.getName()));
        Link i = Link.to(new Reference("scenario.food.berry.Owner"));
        typeNode.setEnclosingClassRef(i);
        typeNode.setOwnerName("scenario.food.berry.Owner");

        ctx.setPackageName("scenario.food.berry");
        Api api = new Api("Test API");
        api.addPackage(pkg);
        api.addType(typeNode);
        pkg.addType(typeNode);
        api.addType(owner);
        resolver = new LinkResolver(api, ctx);
        TextAssembler.assembleTextAndLinks(api, ctx);
        ctx.setApi(api);

        typeWriter.outputTypeDoc(typeNode);
        String output = writer.toString();
        assertTrue(output.contains("Enclosing Class"));
        assertTrue(output.contains("[scenario.food.berry.Owner]"));
        assertTrue(output.contains("Owner.md"));
    }

    @Test
    void outputTypeDoc_fullBody() throws IOException {
        PackageNode pkg = new PackageNode("scenario.food.berry");
        ClassNode typeNode = new ClassNode(ModelUtil.createName("scenario.food.berry.Avocado", pkg.getName()));
        typeNode.setFirstSentence(Text.of("One two three"));

        ctx.setPackageName("scenario.food.berry");
        Api api = new Api("Test API");
        api.addPackage(pkg);
        api.addType(typeNode);
        pkg.addType(typeNode);
        resolver = new LinkResolver(api, ctx);
        TextAssembler.assembleTextAndLinks(api, ctx);
        ctx.setApi(api);

        typeWriter.outputTypeDoc(typeNode);
        String output = writer.toString();
        assertTrue(output.contains("One two three"));
    }

    @Test
    void outputTypeDoc_appliedAnnotations() throws IOException {
        PackageNode pkg = new PackageNode("scenario.food.berry");
        ClassNode typeNode = newClass("Avocado", pkg);

        AnnotationNode at = new AnnotationNode(ModelUtil.createName("scenario.food.berry.Watermelon", "scenarion.food.berry"));
        AppliedAnnotationNode aa = new AppliedAnnotationNode(at.getName().fullyQualifiedName(), at.getPackageName());
        aa.setCustom(true);
        aa.setDocumented(true);
        typeNode.addAppliedAnnotation(aa);
        AnnotationElement element1 = new AnnotationElement(VariableType.parse("Tn"), "Nm", "Va");
        aa.addElement(element1);
        AnnotationElement element2 = new AnnotationElement(VariableType.parse("El"), "Em", "Ent");
        aa.addElement(element2);

        ctx.setPackageName("scenario.food.berry");
        Api api = new Api("Test API");
        api.addPackage(pkg);
        api.addType(typeNode);
        pkg.addType(typeNode);
        resolver = new LinkResolver(api, ctx);
        TextAssembler.assembleTextAndLinks(api, ctx);
        ctx.setApi(api);

        typeWriter.outputTypeDoc(typeNode);

        assertTrue(writer.toString().contains("@Watermelon(Nm Va, Em Ent)"));
    }

    @Test
    void outputDeprecation_record() throws IOException {
        Api api = new Api("Test API");
        PackageNode pkg = new PackageNode("scenario.food.berry");
        api.addPackage(pkg);
        RecordNode typeNode = newRecord("Avocado", pkg);
        api.addType(typeNode);
        pkg.addType(typeNode);

        ctx.setPackageName("scenario.food.berry");
        ctx.setApi(api);

        typeWriter.outputTypeDoc(typeNode);

        assertTrue(writer.toString().contains("Avocado"));
    }

    @Test
    void inheritedMethods() throws IOException {
        // Add a method
        Name methodName = ModelUtil.createName("inheritedMethod", node.getName().fullyQualifiedName(), model.getName());
        MethodNode test = new MethodNode("java.lang.String", methodName);
        test.setOwnerName(node.getName());
        node.addMethod(test);

        // Add a subclass
        ClassNode subClass = newClass("SubClass", model);
        VariableType typeRef = newVariableType("Node");
        Link methodLink = newMethodReference("Node", "inheritedMethod");
        MethodReference methodRef = new MethodReference();
        methodRef.setLink(methodLink);
        List<MethodReference> inheritedMethods = List.of(methodRef);
        subClass.getInheritedMethods().put(typeRef, inheritedMethods);

        typeWriter.outputTypeDoc(subClass);

        System.err.println(writer.toString());
        assertTrue(writer.toString().contains("[inheritedMethod](Node.md#inheritedmethod)"));
    }
}