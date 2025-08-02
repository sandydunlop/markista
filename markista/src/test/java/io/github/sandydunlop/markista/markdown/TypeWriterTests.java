package io.github.sandydunlop.markista.markdown;

import io.github.sandydunlop.markista.model.*;
import io.github.sandydunlop.markista.util.FileUtils;
import io.github.sandydunlop.markista.util.LinkResolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TypeWriterTests {

    FileUtils fileUtils;
    Writer writer;
    TypeWriter typeWriter;

    @BeforeEach
    void setup() throws IOException {
        fileUtils = mock(FileUtils.class);
        // Mock Writer to capture written content
        writer = new StringWriter();
        when(fileUtils.createFile(anyString(), anyString())).thenReturn(writer);
        typeWriter = new TypeWriter(fileUtils);
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
        when(typeNode.getOwner()).thenReturn(null);

        typeWriter.writeDoc(typeNode);

        String content = writer.toString();

        assertTrue(content.contains("# Class MyClass"));
        assertTrue(content.contains("Package [com.example](index.md)"));

        // After writing, typeName should be reset to empty string
        assertEquals("", io.github.sandydunlop.markista.util.Context.getTypeName());
    }

    @Test
    void outputFieldSummary_WritesMarkdownTableForFields() throws IOException {
        PackageNode pkg = new PackageNode("io.github.sandydunlop.markista.model");
        TypeNode typeNode = new TypeNode("io.github.sandydunlop.markista.model.Node", "Node", pkg);
        Api api = mock(Api.class);
        when(api.getTypeNode("com.example.MyEnum")).thenReturn(typeNode);
        LinkResolver.init(api);
        LinkResolver.addNativeModuleUrl("java.base", "http://example.com", ".html");

        // Prepare FieldNode mocks
        FieldNode field1 = mock(FieldNode.class);
        when(field1.getModifiersString()).thenReturn("public ");
        TypeNode field1Type = mock(TypeNode.class);
        when(field1Type.getQualifiedName()).thenReturn("java.lang.String");
        when(field1.getType()).thenReturn(field1Type);
        when(field1.getSimpleName()).thenReturn("fieldOne");
        when(field1.getFirstSentence()).thenReturn(Text.empty().append("Field one description"));
        when(field1.getSince()).thenReturn(Text.empty().append("since"));

        FieldNode field2 = mock(FieldNode.class);
        when(field2.getModifiersString()).thenReturn("private ");
        TypeNode field2Type = mock(TypeNode.class);
        when(field2Type.getQualifiedName()).thenReturn("int");
        when(field2.getType()).thenReturn(field2Type);
        when(field2.getSimpleName()).thenReturn("fieldTwo");
        when(field2.getFirstSentence()).thenReturn(Text.empty().append("Field two description"));
        when(field2.getSince()).thenReturn(Text.empty().append("since"));

        List<FieldNode> fields = new ArrayList<>();
        fields.add(field1);
        fields.add(field2);

        // Use a real StringWriter to verify result
        Writer fieldWriter = new StringWriter();
        when(fileUtils.createFile(anyString(), anyString())).thenReturn(fieldWriter);

        TypeWriter writerInstance = new TypeWriter(fileUtils);
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

        writerInstance.writeDoc(type);

        String output = fieldWriter.toString();

        // The output should contain "Field Summary" heading and the modifiers, field names
        assertTrue(output.contains("## Field Summary"));
        assertTrue(output.contains("public [String]"));
        assertTrue(output.contains("private int"));
        assertTrue(output.contains("fieldOne"));
        assertTrue(output.contains("fieldTwo"));
    }

    @Test
    void outputEnumConstantsSummary_WritesEnumConstantsTable() throws IOException {
        PackageNode pkg = new PackageNode("io.github.sandydunlop.markista.model");
        TypeNode typeNode = new TypeNode("io.github.sandydunlop.markista.model.Node", "Node", pkg);
        Api api = mock(Api.class);
        when(api.getTypeNode("com.example.MyEnum")).thenReturn(typeNode);
        LinkResolver.init(api);

        EnumNode enumNode = mock(EnumNode.class);
        when(enumNode.getQualifiedName()).thenReturn("com.example.MyEnum");
        when(enumNode.getSimpleName()).thenReturn("MyEnum");
        when(enumNode.getPackageName()).thenReturn("com.example");
        when(enumNode.getKind()).thenReturn(TypeNode.Kind.ENUM);

        Reference ref = new Reference(Reference.Kind.URL, "example", "http://example.com");
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

        Writer enumWriter = new StringWriter();
        when(fileUtils.createFile(anyString(), anyString())).thenReturn(enumWriter);

        TypeWriter typewriter = new TypeWriter(fileUtils);
        typewriter.writeDoc(enumNode);

        String output = enumWriter.toString();

        assertTrue(output.contains("##Enum Constants"));
        assertTrue(output.contains("CONST_ONE"));
        assertTrue(output.contains("Constant one desc"));
        assertTrue(output.contains("CONST_TWO"));
        assertTrue(output.contains("Constant two desc"));
    }

    @Test
    void outputEnumConstantDetails_IncludesSinceAndReferences() throws IOException {
        PackageNode pkg = new PackageNode("io.github.sandydunlop.markista.model");
        TypeNode typeNode = new TypeNode("io.github.sandydunlop.markista.model.Node", "Node", pkg);
        Api api = mock(Api.class);
        when(api.getTypeNode("com.example.MyEnum")).thenReturn(typeNode);
        LinkResolver.init(api);

        EnumNode enumNode = mock(EnumNode.class);
        when(enumNode.getQualifiedName()).thenReturn("com.example.MyEnum");

        FieldNode constant = mock(FieldNode.class);
        when(constant.getSimpleName()).thenReturn("CONST");
        when(constant.fullSignature()).thenReturn("CONST");
        when(constant.getFullBody()).thenReturn(Text.empty().append("Full body text for const"));
        when(constant.getSince()).thenReturn(Text.empty().append("Since version 1.0"));

        Reference ref = mock(Reference.class);
        when(constant.getReferences()).thenReturn(List.of(ref));
        when(ref.toString()).thenReturn("ReferenceX");

        List<FieldNode> constants = new ArrayList<>();
        constants.add(constant);
        when(enumNode.getConstants()).thenReturn(constants);

        StringWriter enumConstWriter = new StringWriter();
        when(fileUtils.createFile(anyString(), anyString())).thenReturn(enumConstWriter);

        TypeWriter tw = new TypeWriter(fileUtils);
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

        tw.writeDoc(enumNode);

        String output = enumConstWriter.toString();

        assertTrue(output.contains("### CONST"));
        assertTrue(output.contains("Full body text for const"));
        assertTrue(output.contains("Since version 1.0"));
    }
}