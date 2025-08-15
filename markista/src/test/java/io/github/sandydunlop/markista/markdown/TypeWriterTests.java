package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.*;
import io.github.sandydunlop.markista.util.LinkFormatter;
import io.github.sandydunlop.markista.util.LinkResolver;
import io.github.sandydunlop.markista.util.Markdown;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.source.util.DocTreePath;

import java.io.*;
import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

import jdk.javadoc.doclet.Reporter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TypeWriterTests {
    Writer writer;

    // Mockito will now call the new constructor with this mock
    @InjectMocks
    private TypeWriter typeWriter;

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
    }

    @Test
    void writeDoc_CreatesDocForSimpleClass_TypeNameSetAndReset() throws IOException {
        TypeNode typeNode = mock(TypeNode.class);
        when(typeNode.getQualifiedName()).thenReturn("com.example.MyClass");
        when(typeNode.getKind()).thenReturn(TypeNode.Kind.CLASS);
        when(typeNode.getSimpleName()).thenReturn("MyClass");
        when(typeNode.getPackageName()).thenReturn("com.example");
        // Empty other methods for this simple test
        when(typeNode.getFullBody()).thenReturn(Text.empty());
        when(typeNode.getClasses()).thenReturn(new ArrayList<>());
        when(typeNode.getImplementedInterfaces()).thenReturn(new ArrayList<>());
        when(typeNode.getFields()).thenReturn(new ArrayList<>());
        when(typeNode.getConstructors()).thenReturn(new ArrayList<>());
        when(typeNode.getMethods()).thenReturn(new ArrayList<>());
        when(typeNode.getSupertypes()).thenReturn(new ArrayList<>());
        when(typeNode.getInterfaces()).thenReturn(new ArrayList<>());
        when(typeNode.getEnums()).thenReturn(new ArrayList<>());
        when(typeNode.getAnnotations()).thenReturn(new ArrayList<>());
        when(typeNode.getOwner()).thenReturn("com.example");
        when(typeNode.getKindName()).thenReturn("Class");

        Reference enclosing = Reference.to("com.example.AClass");
        TypeNode typeNode2 = new TypeNode("com.example.MyClass","MyClass","com.example");
        typeNode2.setEnclosingClassRef(enclosing);
        Api api = mock(Api.class);
        when(api.getTypeNode(any())).thenReturn(typeNode2);
        when (contextMock.getApi()).thenReturn(api);

        typeWriter.outputTypeDoc(typeNode);

        String content = writer.toString();

        assertTrue(content.contains("# Class MyClass"));
        assertTrue(content.contains("Package [com.example](index.md)"));
    }

    @Test
    void outputFieldSummary_WritesMarkdownTableForFields() throws IOException {
        FieldNode field1 = new FieldNode("java.lang.String", "fieldOne");
        field1.addModifier(Modifier.PUBLIC);

        FieldNode field2 = new FieldNode("int", "fieldTwo");
        field2.addModifier(Modifier.PRIVATE);

        List<FieldNode> fields = new ArrayList<>();
        fields.add(field1);
        fields.add(field2);

        // invoke the private outputFieldSummary via reflection or use a public method that calls it.
        // Since outputFieldSummary is private, test via writeDoc for a TypeNode with fields

        TypeNode type = mock(TypeNode.class);
        when(type.getSimpleName()).thenReturn("MyClass");
        when(type.getPackageName()).thenReturn("com.example");
        when(type.getKind()).thenReturn(TypeNode.Kind.CLASS);
        when(type.getQualifiedName()).thenReturn("com.example.MyClass");
        when(type.getFields()).thenReturn(fields);
        when(type.getFullBody()).thenReturn(Text.empty());
        when(type.getClasses()).thenReturn(new ArrayList<>());
        when(type.getImplementedInterfaces()).thenReturn(new ArrayList<>());
        when(type.getConstructors()).thenReturn(new ArrayList<>());
        when(type.getMethods()).thenReturn(new ArrayList<>());
        when(type.getSupertypes()).thenReturn(new ArrayList<>());
        when(type.getInterfaces()).thenReturn(new ArrayList<>());
        when(type.getEnums()).thenReturn(new ArrayList<>());
        when(type.getAnnotations()).thenReturn(new ArrayList<>());
        when(type.getOwner()).thenReturn(null);

        when(contextMock.getPackageName()).thenReturn("com.example");
        Api testApi = new Api("Test API");
        testApi.addType(type);
        when(contextMock.getApi()).thenReturn(testApi);
        LinkResolver.init(testApi, contextMock);
        LinkResolver.addNativeModules();
        LinkResolver.addNativeModuleUrl("java.base", "http://example.com", ".html");
        LinkFormatter.generateLinkTexts(testApi, contextMock);

        typeWriter.outputTypeDoc(type);

        String output = writer.toString();

        // The output should contain "Field Summary" heading and the modifiers, field names
        assertTrue(output.contains("## Field Summary"));
        assertTrue(output.contains("public [String]"));
        assertTrue(output.contains("private int"));
        assertTrue(output.contains("fieldOne"));
        assertTrue(output.contains("fieldTwo"));
    }

    @Test
    void outputEnumConstantsSummary_WritesEnumConstantsTable() throws IOException {
        Api api = mock(Api.class);
        LinkResolver.init(api, contextMock);

        EnumTypeNode enumNode = mock(EnumTypeNode.class);
        when(enumNode.getQualifiedName()).thenReturn("com.example.MyEnum");
        when(enumNode.getSimpleName()).thenReturn("MyEnum");
        when(enumNode.getPackageName()).thenReturn("com.example");
        when(enumNode.getKind()).thenReturn(TypeNode.Kind.ENUM);

        Reference ref = new Reference(Reference.Kind.URL, "example", "http://example.com");
        ref.setTarget(ref.getUri());
        List<Reference> references = List.of(ref);

        FieldNode constant1 = mock(FieldNode.class);
        when(constant1.getSimpleName()).thenReturn("CONST_ONE");
        when(constant1.getFirstSentence()).thenReturn(Text.empty().append("Constant one desc"));
        when(constant1.getSince()).thenReturn(Text.empty().append("Constant one since"));
        when(constant1.getReferences()).thenReturn(references);

        FieldNode constant2 = mock(FieldNode.class);
        when(constant2.getSimpleName()).thenReturn("CONST_TWO");
        when(constant2.getFirstSentence()).thenReturn(Text.empty().append("Constant two desc"));
        when(constant2.getSince()).thenReturn(Text.empty().append("Constant two since"));
        when(constant2.getReferences()).thenReturn(references);

        List<FieldNode> constants = new ArrayList<>();
        constants.add(constant1);
        constants.add(constant2);

        when(enumNode.getConstants()).thenReturn(constants);
        when(enumNode.getFullBody()).thenReturn(Text.empty());
        when(enumNode.getClasses()).thenReturn(new ArrayList<>());
        when(enumNode.getImplementedInterfaces()).thenReturn(new ArrayList<>());
        when(enumNode.getConstructors()).thenReturn(new ArrayList<>());
        when(enumNode.getMethods()).thenReturn(new ArrayList<>());
        when(enumNode.getSupertypes()).thenReturn(new ArrayList<>());
        when(enumNode.getInterfaces()).thenReturn(new ArrayList<>());
        when(enumNode.getEnums()).thenReturn(new ArrayList<>());
        when(enumNode.getAnnotations()).thenReturn(new ArrayList<>());
        when(enumNode.getOwner()).thenReturn(null);
        when(contextMock.getApi()).thenReturn(api);

        LinkResolver.init(api, contextMock);
        LinkResolver.addNativeModules();
        LinkFormatter.generateLinkTexts(api, contextMock);
        Markdown.setContext(contextMock);

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
        Api api = mock(Api.class);
        LinkResolver.init(api, contextMock);

        EnumTypeNode enumNode = mock(EnumTypeNode.class);
        when(enumNode.getQualifiedName()).thenReturn("com.example.MyEnum");

        FieldNode constant = mock(FieldNode.class);
        when(constant.getSimpleName()).thenReturn("CONST");
        when(constant.fullSignature()).thenReturn("CONST");
        when(constant.getFullBody()).thenReturn(Text.empty().append("Full body text for const"));
        when(constant.getSince()).thenReturn(Text.empty().append("Since version 1.0"));

        Reference ref = Reference.to("io.github.sandydunlop.markista.model.Node")
                .withKind(Reference.Kind.TYPE)
                .withLabel("See ALso");
        when(constant.getReferences()).thenReturn(List.of(ref));

        List<FieldNode> constants = new ArrayList<>();
        constants.add(constant);
        when(enumNode.getConstants()).thenReturn(constants);

        // Calling outputEnumConstantDetails is private; access via writeDoc on an enum with constants
        when(enumNode.getSimpleName()).thenReturn("MyEnum");
        when(enumNode.getKind()).thenReturn(TypeNode.Kind.ENUM);
        when(enumNode.getPackageName()).thenReturn("com.example");
        when(enumNode.getFullBody()).thenReturn(Text.empty());
        when(enumNode.getClasses()).thenReturn(new ArrayList<>());
        when(enumNode.getImplementedInterfaces()).thenReturn(new ArrayList<>());
        when(enumNode.getConstructors()).thenReturn(new ArrayList<>());
        when(enumNode.getMethods()).thenReturn(new ArrayList<>());
        when(enumNode.getSupertypes()).thenReturn(new ArrayList<>());
        when(enumNode.getInterfaces()).thenReturn(new ArrayList<>());
        when(enumNode.getEnums()).thenReturn(new ArrayList<>());
        when(enumNode.getAnnotations()).thenReturn(new ArrayList<>());
        when(enumNode.getOwner()).thenReturn(null);

        Api testApi = new Api("Test API");
        when(contextMock.getApi()).thenReturn(testApi);
        LinkResolver.init(testApi, contextMock);
        LinkResolver.addNativeModules();
        LinkResolver.addNativeModuleUrl("java.base", "http://example.com", ".html");
        LinkFormatter.generateLinkTexts(testApi, contextMock);

        typeWriter.outputTypeDoc(enumNode);

        String output = writer.toString();

        assertTrue(output.contains("### CONST"));
        assertTrue(output.contains("Full body text for const"));
        assertTrue(output.contains("Since version 1.0"));
    }

    @Test
    void outputDeclaration() throws IOException {
        PackageNode pkg = new PackageNode("scenario.food.category");
        AnnotationTypeNode typeNode = new AnnotationTypeNode("scenario.food.category.SaladIngredient", "SaladIngredient", 
                pkg.getQualifiedName());

        AppliedAnnotationNode appliedAnnotation = new AppliedAnnotationNode("Target");
        AnnotationElement param = new AnnotationElement(null, "value", ElementType.TYPE.toString());
        appliedAnnotation.addElement(param);

        when(contextMock.getApi()).thenReturn(new Api(""));
        typeNode.addAppliedAnnotation(appliedAnnotation);

        typeWriter.outputTypeDoc(typeNode);
        String output = writer.toString();
        assertTrue(output.contains("@interface __SaladIngredient__"));
    }

    @Test
    void outputDeprecation() throws IOException {
        PackageNode pkg = new PackageNode("scenario.food.berry");
        ClassTypeNode typeNode = new ClassTypeNode("scenario.food.berry.Avocado", "Avocado", 
                pkg.getQualifiedName());

        MethodNode methodNode = new MethodNode("java.lang.String", "eat");

        methodNode.setDeprecation(Deprecation.DEPRECATED);
        methodNode.setDeprecationText(Text.empty().append("This is deprecated"));
        typeNode.addMethod(methodNode);
        when(contextMock.getApi()).thenReturn(new Api(""));

        typeWriter.outputTypeDoc(typeNode);
        String output = writer.toString();
        assertTrue(output.contains("This is deprecated"));
    }

    @Test
    void outputDeprecation_noText() throws IOException {
        PackageNode pkg = new PackageNode("scenario.food.berry");
        ClassTypeNode typeNode = new ClassTypeNode("scenario.food.berry.Avocado", "Avocado", 
                pkg.getQualifiedName());

        MethodNode methodNode = new MethodNode("java.lang.String", "eat");

        methodNode.setDeprecation(Deprecation.DEPRECATED);
        typeNode.addMethod(methodNode);

        Context ctx = Context.getInstance();
        ctx.setPackageName("scenario.food.berry");
        Api api = new Api("Test API");
        api.addPackage(pkg);
        api.addType(typeNode);
        pkg.addType(typeNode);
        LinkResolver.init(api, ctx);
        when(contextMock.getApi()).thenReturn(api);

        typeWriter.outputTypeDoc(typeNode);
        String output = writer.toString();
        assertTrue(output.contains("This has been marked as deprecated"));
    }

    @Test
    void outputMethodParams() throws IOException {
        PackageNode pkg = new PackageNode("scenario.food.berry");
        ClassTypeNode typeNode = new ClassTypeNode("scenario.food.berry.Avocado", "Avocado", 
                pkg.getQualifiedName());
        MethodNode methodNode = new MethodNode("java.lang.String", "eat");

        ParamNode param1 = new ParamNode("java.lang.String", "param1");
        param1.setFullBody(Text.empty().append("param1doc"));
        methodNode.addParam(param1);
        ParamNode param2 = new ParamNode("java.lang.String", "param2");
        param2.setFullBody(Text.empty().append("param2doc"));
        methodNode.addParam(param2);

        typeNode.addMethod(methodNode);

        Context ctx = Context.getInstance();
        ctx.setPackageName("scenario.food.berry");
        Api api = new Api("Test API");
        api.addPackage(pkg);
        api.addType(typeNode);
        pkg.addType(typeNode);
        LinkResolver.init(api, ctx);
        LinkResolver.addNativeModules(); // Needed for String
        LinkFormatter.generateLinkTexts(api, ctx);
        when(contextMock.getApi()).thenReturn(api);

        typeWriter.outputTypeDoc(typeNode);
        String output = writer.toString();
        assertTrue(output.contains("[eat](#eat)([String](https"));
        assertTrue(output.contains("param2doc"));
    }

    @Test
    void outputMethodDetails() throws IOException {
        PackageNode pkg = new PackageNode("scenario.food.berry");
        ClassTypeNode typeNode = new ClassTypeNode("scenario.food.berry.Avocado", "Avocado", 
                pkg.getQualifiedName());
        MethodNode methodNode = new MethodNode("java.lang.String", "eat");

        Reference thrownRef = Reference.to("scenario.food.berry.Thrown")
                .withKind(Reference.Kind.TYPE)
                .withLabel("scenario.food.berry.Thrown");
        Reference specifiedByRef = Reference.to("scenario.food.berry.Specified")
                .withKind(Reference.Kind.TYPE)
                .withLabel("scenario.food.berry.Specified");
        ClassTypeNode specifiedByType = new ClassTypeNode("scenario.food.berry.Specified", "Specified", 
                pkg.getQualifiedName());
        ClassTypeNode thrownType = new ClassTypeNode("scenario.food.berry.Thrown", "Thrown", 
                pkg.getQualifiedName());
        
        methodNode.setReturnDescription(Text.empty().append("returnDescription"));
        methodNode.setSpecifiedBy(specifiedByRef);
        methodNode.addThrownType(thrownRef);
        OverriddenMethodNode overriddenMethod = new OverriddenMethodNode("scenario.food.berry.Avocado", "eat");
        methodNode.setOverriddenMethod(overriddenMethod);

        typeNode.addMethod(methodNode);

        Context ctx = Context.getInstance();
        ctx.setPackageName("scenario.food.berry");
        Api api = new Api("Test API");
        api.addPackage(pkg);
        api.addType(typeNode);
        pkg.addType(typeNode);
        api.addType(specifiedByType);
        api.addType(thrownType);
        LinkResolver.init(api, ctx);
        when(contextMock.getApi()).thenReturn(api);

        LinkResolver.addNativeModules();
        LinkFormatter.generateLinkTexts(api, ctx);

        typeWriter.outputTypeDoc(typeNode);
        String output = writer.toString();
        assertTrue(output.contains("returnDescription"));
        assertTrue(output.contains("Specified.md"));
        assertTrue(output.contains("Thrown.md"));
        assertTrue(output.contains("eat"));
    }
}