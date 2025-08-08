package io.github.sandydunlop.markista.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;
import com.sun.source.doctree.StartElementTree;

import io.github.sandydunlop.markista.model.AnnotationTypeNode;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassTypeNode;
import io.github.sandydunlop.markista.model.EnumTypeNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.InterfaceTypeNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.Node;
import io.github.sandydunlop.markista.model.OverriddenMethodNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.Text.SegmentKind;
import io.github.sandydunlop.markista.model.TypeNode;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


class TypeUtilsTests {
    private static Context ctx;
    private Api dummyApi;
    private PackageNode dummyPackage;

    private Api api;
    private PackageNode packageNode;
    private ModuleNode unnamedModule = new ModuleNode("");

    private Elements elementUtils;
    private Types typeUtils;
    private DocTrees treeUtils;

    private TypeElement typeElement;
    private TypeMirror typeMirror;
    private DocCommentTree docCommentTree;
    private DocTree dt;

    private DocletEnvironment docletEnv;
    private Name name;
    private Name qualifiedName;
    private PackageElement packageElement;

    DocletEnvironment environment;
      
    DocletEnvironment mockEnvironment;
    Api mockApi;
    Context mockContext;
    Name simpleName2;
    Name qualifiedName2;
    Name packageName2;

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
		ctx =  Context.getInstance();
		ctx.setReporter(reporter);
    }

    @BeforeEach
    void init() {
        dummyApi = new Api("Test API");
        dummyPackage = new PackageNode("com.example");

        environment = mock(DocletEnvironment.class);
        elementUtils = mock(Elements.class);
        typeUtils = mock(Types.class);
        treeUtils = mock(DocTrees.class);

        docletEnv = mock(DocletEnvironment.class);
        when(docletEnv.getElementUtils()).thenReturn(elementUtils);
        when(docletEnv.getTypeUtils()).thenReturn(typeUtils);
        when(docletEnv.getDocTrees()).thenReturn(treeUtils);

        name = mock(Name.class);
        qualifiedName = mock(Name.class);

        // A package...
        Name packageName = mock(Name.class);
        when(packageName.toString()).thenReturn("io.github.sandydunlop.markista.model");
        packageElement = mock(PackageElement.class);
        when(packageElement.getQualifiedName()).thenReturn(packageName);
        when(packageElement.getKind()).thenReturn(ElementKind.PACKAGE);

        // A class called "Node" (used by class, method, and field tests)
        TypeMirror superTypeMirror = mock(TypeMirror.class);
        typeMirror = mock(TypeMirror.class);
        typeElement = mock(TypeElement.class);
        when(typeElement.getKind()).thenReturn(ElementKind.CLASS);
        when(typeElement.getSimpleName()).thenReturn(name);
        when(typeElement.getQualifiedName()).thenReturn(qualifiedName);
        when(typeElement.getEnclosingElement()).thenReturn(packageElement);
        when(typeElement.asType()).thenReturn(typeMirror);
        List<? extends TypeMirror> superTypes = List.of(superTypeMirror);
        when(typeUtils.directSupertypes(typeMirror)).thenAnswer(_ -> superTypes);
        when(name.toString()).thenReturn("Node");
        when(qualifiedName.toString()).thenReturn("io.github.sandydunlop.markista.model.Node");

        // A doc comment tree
        dt = mock(DocTree.class);
        when(dt.toString()).thenReturn("berry");
        when(dt.getKind()).thenReturn(com.sun.source.doctree.DocTree.Kind.TEXT);
        List<? extends DocTree> dtList = List.of(dt);
        docCommentTree = mock(DocCommentTree.class);
        when(treeUtils.getDocCommentTree(typeElement)).thenReturn(docCommentTree);
        when(docCommentTree.getKind()).thenReturn(com.sun.source.doctree.DocTree.Kind.END_ELEMENT);
        when(docCommentTree.toString()).thenReturn("");
        when(docCommentTree.getFirstSentence()).thenAnswer(_ -> dtList);
        when(docCommentTree.getBody()).thenAnswer(_ -> dtList);
        when(docCommentTree.getFullBody()).thenAnswer(_ -> dtList);

        api = new Api("Test API");
        TypeUtils.init(api, docletEnv);

        packageNode = new PackageNode("io.github.sandydunlop.markista.model");
        api.addPackage(packageNode);
    }

    @Test
    void addConstantFieldValuesReference() {
        ClassTypeNode classNode2 = new ClassTypeNode("io.github.sandydunlop.markista.model.TypeNode", "Node", packageNode);
        classNode2.getSupertypes().add("io.github.sandydunlop.markista.model.Node");
        api.addType(classNode2);
        TypeNode typeNode = new TypeNode("int","int", packageNode);
        FieldNode fieldNode = new FieldNode(typeNode, "field");
        fieldNode.setConstantValue("1");
        classNode2.addField(fieldNode);
        TypeUtils.addConstantFieldValuesReference(unnamedModule);
        assertEquals(1, unnamedModule.getConstantValues().size());
    }

    @Test
    void getOverriddenMethod_local() {
        Name methodName1 = mock(Name.class);
        when(methodName1.toString()).thenReturn("length");
        ExecutableElement methodElement1 = mock(ExecutableElement.class);
        when(methodElement1.getReturnType()).thenReturn(typeMirror);
        when(methodElement1.getSimpleName()).thenReturn(methodName1);
        when(methodElement1.getEnclosingElement()).thenReturn(typeElement);

        Name methodName2 = mock(Name.class);
        when(methodName2.toString()).thenReturn("length");
        ExecutableElement methodElement2 = mock(ExecutableElement.class);
        when(methodElement2.getReturnType()).thenReturn(typeMirror);
        when(methodElement2.getSimpleName()).thenReturn(methodName2);
        when(methodElement2.getEnclosingElement()).thenReturn(typeElement);

        ClassTypeNode classNode1 = new ClassTypeNode("io.github.sandydunlop.markista.model.Node", "Node", packageNode);
        api.addType(classNode1);
        TypeNode returnType1 = new TypeNode("int","int", null);
        MethodNode methodNode1 = new MethodNode(returnType1, "length");
        methodNode1.setOwner(classNode1);
        classNode1.addMethod(methodNode1);

        ClassTypeNode classNode2 = new ClassTypeNode("io.github.sandydunlop.markista.model.TypeNode", "Node", packageNode);
        classNode2.getSupertypes().add("io.github.sandydunlop.markista.model.Node");
        api.addType(classNode2);
        TypeNode returnType2 = new TypeNode("int","int", null);
        MethodNode methodNode2 = new MethodNode(returnType2, "length");
        methodNode2.setOwner(classNode2);
        classNode2.addMethod(methodNode2);

        when(elementUtils.getTypeElement("io.github.sandydunlop.markista.model.Node")).thenAnswer(_ -> typeElement);
        List<? extends Element> enclosedElements = List.of(methodElement1);
        when(typeElement.getEnclosedElements()).thenAnswer(_ -> enclosedElements);

        OverriddenMethodNode om = TypeUtils.getOverriddenMethod(methodNode2, methodElement2);
        assertNotNull(om);
        assertEquals("io.github.sandydunlop.markista.model.Node", om.getClassName());
        assertEquals("length", om.getMethodName());
    }

    @Test
    void getOverriddenMethod_native() {
        Name methodName = mock(Name.class);
        when(methodName.toString()).thenReturn("length");
        ExecutableElement methodElement = mock(ExecutableElement.class);
        when(methodElement.getReturnType()).thenReturn(typeMirror);
        when(methodElement.getSimpleName()).thenReturn(methodName);
        when(methodElement.getEnclosingElement()).thenReturn(typeElement);

        ClassTypeNode classNode = new ClassTypeNode("io.github.sandydunlop.markista.model.Node", "Node", packageNode);
        classNode.getSupertypes().add("java.lang.String");
        api.addType(classNode);

        TypeNode returnType = new TypeNode("int","int", null);
        MethodNode methodNode = new MethodNode(returnType, "length");
        methodNode.setOwner(classNode);
        classNode.addMethod(methodNode);

        OverriddenMethodNode om = TypeUtils.getOverriddenMethod(methodNode, methodElement);
        assertNotNull(om);
        assertEquals("java.lang.String", om.getClassName());
        assertEquals("length", om.getMethodName());
    }

    @Test
    void getFieldType() {
        TypeMirror tm = mock(TypeMirror.class);
        when(tm.toString()).thenAnswer(_ -> "int");

        Name param1name = mock(Name.class);
        when(param1name.toString()).thenReturn("param1");
        VariableElement param1 = mock(VariableElement.class);
        when(param1.asType()).thenAnswer(_ -> tm);
        when(param1.getSimpleName()).thenAnswer(_ -> param1name);
        when(param1.getKind()).thenAnswer(_ -> ElementKind.FIELD);
        List<? extends Element>  parameters = List.of(param1);

        when(typeElement.getEnclosedElements()).thenAnswer(_ -> parameters);
        when(elementUtils.getTypeElement("io.github.sandydunlop.markista.model.Node")).thenAnswer(_ -> typeElement);

        TypeNode tn = TypeUtils.getFieldType("io.github.sandydunlop.markista.model.Node", "param1");
        assertNotNull(tn);
        assertEquals("int", tn.getSimpleName());
    }

    @Test
    void getFieldType_array() {
        TypeMirror tm = mock(TypeMirror.class);
        when(tm.toString()).thenAnswer(_ -> "int[]");
        when(tm.getKind()).thenAnswer(_ -> TypeKind.ARRAY);

        ArrayType at = mock(ArrayType.class);
        when(at.toString()).thenAnswer(_ -> "int[]");
        when(at.getKind()).thenAnswer(_ -> TypeKind.ARRAY);
        when(at.toString()).thenAnswer(_ -> "int");
        when (at.getComponentType()).thenReturn((TypeMirror)at);

        Name param1name = mock(Name.class);
        when(param1name.toString()).thenReturn("param1");
        VariableElement param1 = mock(VariableElement.class);
        when(param1.asType()).thenAnswer(_ -> tm);
        when(param1.getSimpleName()).thenAnswer(_ -> param1name);
        when(param1.getKind()).thenAnswer(_ -> ElementKind.FIELD);
        List<? extends Element>  parameters = List.of(param1);

        when(typeElement.getEnclosedElements()).thenAnswer(_ -> parameters);
        when(elementUtils.getTypeElement("io.github.sandydunlop.markista.model.Node")).thenAnswer(_ -> typeElement);

        when (typeUtils.getArrayType(any())).thenReturn(at);

        TypeNode tn = TypeUtils.getFieldType("io.github.sandydunlop.markista.model.Node", "param1");
        assertNotNull(tn);
        assertEquals("int", tn.getSimpleName());
    }

    @Test
    void getParamType() {
        TypeMirror array = mock(TypeMirror.class);
        when(array.toString()).thenAnswer(_ -> "int");

        ArrayType at = mock(ArrayType.class);
        when(at.getKind()).thenAnswer(_ -> TypeKind.ARRAY);
        when(at.getComponentType()).thenAnswer(_ -> array);

        Name param1name = mock(Name.class);
        when(param1name.toString()).thenReturn("param1");
        VariableElement param1 = mock(VariableElement.class);
        when(param1.asType()).thenAnswer(_ -> at);
        when(param1.getSimpleName()).thenAnswer(_ -> param1name);
        List<? extends VariableElement>  parameters = List.of(param1);
        Name methodName = mock(Name.class);
        when(methodName.toString()).thenReturn("method");
        ExecutableElement methodElement = mock(ExecutableElement.class);
        when(methodElement.getReturnType()).thenReturn(typeMirror);
        when(methodElement.getSimpleName()).thenReturn(methodName);
        when(methodElement.getEnclosingElement()).thenReturn(typeElement);
        when(methodElement.getParameters()).thenAnswer(_ -> parameters);

        TypeNode tn = TypeUtils.getParamType(methodElement, "param1");
        assertNotNull(tn);
        assertEquals("int", tn.getSimpleName());
        assertEquals("\\[]", tn.getArrayBrackets());
    }

    @Test
    void getReferences() {
        DocTree referenceTree1 = mock(DocTree.class);
        when(referenceTree1.getKind()).thenAnswer(_ -> com.sun.source.doctree.DocTree.Kind.MARKDOWN);
        when(referenceTree1.toString()).thenAnswer(_ -> "<a href=\"http://example.com\">");
        
        DocTree referenceTree2 = mock(DocTree.class);
        when(referenceTree2.getKind()).thenAnswer(_ -> com.sun.source.doctree.DocTree.Kind.REFERENCE);
        when(referenceTree2.toString()).thenAnswer(_ -> "Node");

        List<? extends DocTree> refsList = List.of(referenceTree1, referenceTree2);
        
        SeeTree seeTree = mock(SeeTree.class);
        when(seeTree.getReference()).thenAnswer(_ -> refsList);
        List<? extends DocTree> blockTags = List.of(seeTree);
        when(docCommentTree.getBlockTags()).thenAnswer(_ -> blockTags);

        List<Reference> refs = TypeUtils.getReferences(docCommentTree);
        assertNotNull(refs);
        assertEquals(2, refs.size());
        assertEquals(Reference.Kind.URL, refs.get(0).getKind());
        assertEquals("http://example.com", refs.get(0).getUri());
        assertEquals(Reference.Kind.TYPE, refs.get(1).getKind());
        assertEquals("Node", refs.get(1).getTarget());
    }

    @Test
    void getUrl() {
        String url = TypeUtils.getUrl("<a href=\"http://example.com\">text</a>");
        assertEquals("http://example.com", url);
    }

    @Test
    void nodeFromElement_TypeElement_Class() {
        TypeNode node = TypeUtils.nodeFromElement(typeElement);
        assertNotNull(node);
        assertEquals("Node", node.getSimpleName());
        assertEquals("io.github.sandydunlop.markista.model.Node", node.getQualifiedName());
        assertEquals(TypeNode.Kind.CLASS, node.getKind());
    }

    @Test
    void nodeFromElement_TypeElement_Interface() {
        TypeMirror interfaceSuperTypeMirror = mock(TypeMirror.class);
        TypeMirror interfaceTypeMirror = mock(TypeMirror.class);
        TypeElement interfaceTypeElement = mock(TypeElement.class);
        when(interfaceTypeElement.getKind()).thenReturn(ElementKind.INTERFACE);
        when(interfaceTypeElement.getSimpleName()).thenReturn(name);
        when(interfaceTypeElement.getQualifiedName()).thenReturn(qualifiedName);
        when(interfaceTypeElement.getEnclosingElement()).thenReturn(packageElement);
        when(interfaceTypeElement.asType()).thenReturn(interfaceTypeMirror);
        List<? extends TypeMirror> superTypes = List.of(interfaceSuperTypeMirror);
        when(typeUtils.directSupertypes(interfaceTypeMirror)).thenAnswer(_ -> superTypes);
        when(name.toString()).thenReturn("PackageMember");
        when(qualifiedName.toString()).thenReturn("io.github.sandydunlop.markista.model.PackageMember");

        TypeNode node = TypeUtils.nodeFromElement(interfaceTypeElement);
        assertNotNull(node);
        assertEquals("PackageMember", node.getSimpleName());
        assertEquals("io.github.sandydunlop.markista.model.PackageMember", node.getQualifiedName());
        assertEquals(TypeNode.Kind.INTERFACE, node.getKind());
    }

    @Test
    void nodeFromElement_TypeElement_Enum() {
        TypeMirror interfaceSuperTypeMirror = mock(TypeMirror.class);
        TypeMirror interfaceTypeMirror = mock(TypeMirror.class);
        TypeElement enumTypeElement = mock(TypeElement.class);
        when(enumTypeElement.getKind()).thenReturn(ElementKind.ENUM);
        when(enumTypeElement.getSimpleName()).thenReturn(name);
        when(enumTypeElement.getQualifiedName()).thenReturn(qualifiedName);
        when(enumTypeElement.getEnclosingElement()).thenReturn(packageElement);
        when(enumTypeElement.asType()).thenReturn(interfaceTypeMirror);
        List<? extends TypeMirror> superTypes = List.of(interfaceSuperTypeMirror);
        when(typeUtils.directSupertypes(interfaceTypeMirror)).thenAnswer(_ -> superTypes);
        when(name.toString()).thenReturn("Deprecation");
        when(qualifiedName.toString()).thenReturn("io.github.sandydunlop.markista.model.Deprecation");

        TypeNode node = TypeUtils.nodeFromElement(enumTypeElement);
        assertNotNull(node);
        assertEquals("Deprecation", node.getSimpleName());
        assertEquals("io.github.sandydunlop.markista.model.Deprecation", node.getQualifiedName());
        assertEquals(TypeNode.Kind.ENUM, node.getKind());
    }

    @Test
    void nodeFromElement_TypeElement_Annotation() {
        TypeMirror interfaceSuperTypeMirror = mock(TypeMirror.class);
        TypeMirror interfaceTypeMirror = mock(TypeMirror.class);
        TypeElement annotationTypeElement = mock(TypeElement.class);
        when(annotationTypeElement.getKind()).thenReturn(ElementKind.ANNOTATION_TYPE);
        when(annotationTypeElement.getSimpleName()).thenReturn(name);
        when(annotationTypeElement.getQualifiedName()).thenReturn(qualifiedName);
        when(annotationTypeElement.getEnclosingElement()).thenReturn(packageElement);
        when(annotationTypeElement.asType()).thenReturn(interfaceTypeMirror);
        List<? extends TypeMirror> superTypes = List.of(interfaceSuperTypeMirror);
        when(typeUtils.directSupertypes(interfaceTypeMirror)).thenAnswer(_ -> superTypes);
        when(name.toString()).thenReturn("Overrides");
        when(qualifiedName.toString()).thenReturn("io.github.sandydunlop.markista.model.Overrides");

        TypeNode node = TypeUtils.nodeFromElement(annotationTypeElement);
        assertNotNull(node);
        assertEquals("Overrides", node.getSimpleName());
        assertEquals("io.github.sandydunlop.markista.model.Overrides", node.getQualifiedName());
        assertEquals(TypeNode.Kind.ANNOTATION, node.getKind());
    }

    @Test
    void nodeFromElement_ExecutableElement() {
        ClassTypeNode classNode = new ClassTypeNode("io.github.sandydunlop.markista.model.Node", "Node", packageNode);
        api.addType(classNode);

        Name methodName = mock(Name.class);
        when(methodName.toString()).thenReturn("method");
        ExecutableElement methodElement = mock(ExecutableElement.class);
        when(methodElement.getReturnType()).thenReturn(typeMirror);
        when(methodElement.getSimpleName()).thenReturn(methodName);
        when(methodElement.getEnclosingElement()).thenReturn(typeElement);

        DocCommentTree docTree = mock(DocCommentTree.class);
        when(treeUtils.getDocCommentTree(typeElement)).thenReturn(docTree);
        
        MethodNode node = TypeUtils.nodeFromElement(methodElement);
        assertNotNull(node);
    }

    @Test
    void nodeFromElement_VariableElement() {
        ClassTypeNode classNode = new ClassTypeNode("io.github.sandydunlop.markista.model.Node", "Node", packageNode);
        api.addType(classNode);

        Name variableName = mock(Name.class);
        when(variableName.toString()).thenReturn("berry");
        VariableElement variableElement = mock(VariableElement.class);
        when(variableElement.getEnclosingElement()).thenReturn(typeElement);
        when(variableElement.getSimpleName()).thenReturn(variableName);
        when(elementUtils.getTypeElement(classNode.getQualifiedName())).thenReturn(typeElement);

        DocCommentTree docTree = mock(DocCommentTree.class);
        when(treeUtils.getDocCommentTree(typeElement)).thenReturn(docTree);

        FieldNode node = TypeUtils.nodeFromElement(variableElement);
        assertNotNull(node);
    }

    @Test
    void createText_MARKDOWN() {
        DocCommentTree docTree = mock(DocCommentTree.class);
        when(treeUtils.getDocCommentTree(typeElement)).thenReturn(docTree);
        when(docTree.getKind()).thenReturn(com.sun.source.doctree.DocTree.Kind.MARKDOWN);
        when(docTree.toString()).thenReturn("berry");
        List<? extends DocTree> dtList = List.of(docTree);
        Text text = TypeUtils.createText(dtList);
        assertNotNull(text);
        assertEquals(1, text.getSegments().size());
        assertEquals(Text.SegmentKind.MARKDOWN, text.getSegments().get(0).getKind());
        assertEquals("berry", text.toString());
    }

    @Test
    void createText_LINK() {
        DocCommentTree docTree = mock(DocCommentTree.class);
        when(treeUtils.getDocCommentTree(typeElement)).thenReturn(docTree);
        when(docTree.getKind()).thenReturn(com.sun.source.doctree.DocTree.Kind.LINK);
        when(docTree.toString()).thenReturn("{@link http://example.com}");
        List<? extends DocTree> dtList = List.of(docTree);
        Text text = TypeUtils.createText(dtList);
        assertNotNull(text);
        assertEquals(1, text.getSegments().size());
        assertEquals(Text.SegmentKind.LINK, text.getSegments().get(0).getKind());
        assertEquals("http://example.com", text.getSegments().get(0).getLink());
    }

    @Test
    void createText_TEXT() {
        DocCommentTree docTree = mock(DocCommentTree.class);
        when(treeUtils.getDocCommentTree(typeElement)).thenReturn(docTree);
        when(docTree.getKind()).thenReturn(com.sun.source.doctree.DocTree.Kind.TEXT);
        when(docTree.toString()).thenReturn("berry");
        List<? extends DocTree> dtList = List.of(docTree);
        Text text = TypeUtils.createText(dtList);
        assertNotNull(text);
        assertEquals(1, text.getSegments().size());
        assertEquals(Text.SegmentKind.TEXT, text.getSegments().get(0).getKind());
        assertEquals("berry", text.toString());
    }

    @Test
    void createText_CODE() {
        DocCommentTree docTree = mock(DocCommentTree.class);
        when(treeUtils.getDocCommentTree(typeElement)).thenReturn(docTree);
        when(docTree.getKind()).thenReturn(com.sun.source.doctree.DocTree.Kind.CODE);
        when(docTree.toString()).thenReturn("{@code berry}");
        List<? extends DocTree> dtList = List.of(docTree);
        Text text = TypeUtils.createText(dtList);
        assertNotNull(text);
        assertEquals(1, text.getSegments().size());
        assertEquals(Text.SegmentKind.CODE, text.getSegments().get(0).getKind());
        assertEquals("berry", text.toString());
    }

    @Test
    void createText_START() {
        StartElementTree set = mock(StartElementTree.class);
        Name setName = mock(Name.class);
        when(setName.toString()).thenReturn("p");
        when(set.getName()).thenReturn(setName);        
        when(set.getKind()).thenReturn(com.sun.source.doctree.DocTree.Kind.START_ELEMENT);
        when(set.toString()).thenReturn("<p>");
        List<? extends DocTree> dtList = List.of(set);
        Text text = TypeUtils.createText(dtList);
        assertNotNull(text);
        assertEquals(1, text.getSegments().size());
        assertEquals(Text.SegmentKind.START, text.getSegments().get(0).getKind());
        assertEquals("\n\n", text.toString());
    }

    @Test
    void createText_END() {
        DocCommentTree docTree = mock(DocCommentTree.class);
        when(treeUtils.getDocCommentTree(typeElement)).thenReturn(docTree);
        when(docTree.getKind()).thenReturn(com.sun.source.doctree.DocTree.Kind.END_ELEMENT);
        when(docTree.toString()).thenReturn("");
        List<? extends DocTree> dtList = List.of(docTree);
        Text text = TypeUtils.createText(dtList);
        assertNotNull(text);
        assertEquals(1, text.getSegments().size());
        assertEquals(Text.SegmentKind.END, text.getSegments().get(0).getKind());
        assertEquals("", text.toString());
    }

    @Test
    void setDocumentation() {
        ClassTypeNode classNode = new ClassTypeNode("io.github.sandydunlop.markista.model.Node", "Node", packageNode);
        api.addType(classNode);
        TypeUtils.setDocumentation(classNode, typeElement);
        assertEquals("berry", Markdown.formatText(classNode.getFirstSentence()));
        assertEquals("berry", Markdown.formatText(classNode.getBody()));
        assertEquals("berry", Markdown.formatText(classNode.getFullBody()));
    }

    @Test
    void testCreateTypeNodeByKind() {
        PackageNode pkg = new PackageNode("test.pkg");

        TypeNode classNode = TypeUtils.createTypeNode("test.pkg.ClassA", "ClassA", pkg, ElementKind.CLASS);
        assertNotNull(classNode);
        assertTrue(classNode instanceof ClassTypeNode);
        assertEquals("ClassA", classNode.getSimpleName());

        TypeNode interfaceNode = TypeUtils.createTypeNode("test.pkg.InterfaceA", "InterfaceA", pkg, ElementKind.INTERFACE);
        assertNotNull(interfaceNode);
        assertTrue(interfaceNode instanceof InterfaceTypeNode);

        TypeNode enumNode = TypeUtils.createTypeNode("test.pkg.EnumA", "EnumA", pkg, ElementKind.ENUM);
        assertNotNull(enumNode);
        assertTrue(enumNode instanceof EnumTypeNode);

        TypeNode annoNode = TypeUtils.createTypeNode("test.pkg.AnnotationA", "AnnotationA", pkg, ElementKind.ANNOTATION_TYPE);
        assertNotNull(annoNode);
        assertTrue(annoNode instanceof AnnotationTypeNode);

        TypeNode nullNode = TypeUtils.createTypeNode("test.pkg.UnknownA", "UnknownA", pkg, ElementKind.METHOD);
        assertNull(nullNode);
    }

    @Test
    void testGetUrl() {
        String html = "<a href=\"http://example.com\">link</a>";
        String url = TypeUtils.getUrl(html);
        assertEquals("http://example.com", url);

        url = TypeUtils.getUrl("noQuotes");
        assertNull(url);

        url = TypeUtils.getUrl(null);
        assertNull(url);
    }

    @Test
    void testSetModifiers() {
        Node node = mock(Node.class);
        Set<javax.lang.model.element.Modifier> mods = new HashSet<>();
        mods.add(javax.lang.model.element.Modifier.PUBLIC);
        mods.add(javax.lang.model.element.Modifier.STATIC);

        TypeUtils.setModifiers(node, mods);

        // verify addModifier called with corresponding enum values
        verify(node, times(mods.size())).addModifier(any());
    }

    @Test
    void testCreateText_and_createTextSegment() {
        List<com.sun.source.doctree.DocTree> docTreeList = new ArrayList<>();

        com.sun.source.doctree.DocTree textTree = mock(com.sun.source.doctree.DocTree.class);
        when(textTree.getKind()).thenReturn(com.sun.source.doctree.DocTree.Kind.TEXT);
        when(textTree.toString()).thenReturn("simple text");

        docTreeList.add(textTree);

        Text text = TypeUtils.createText(docTreeList);
        assertNotNull(text);
        assertTrue(text.getSegments().stream().anyMatch(s -> s.getText().contains("simple text")));

        Text.Segment segment = TypeUtils.createTextSegment(textTree);
        assertEquals(SegmentKind.TEXT, segment.getKind());
        assertEquals("simple text", segment.getText());
    }

    @Test
    void createTypeNode_ReturnsClassNode_WhenElementKindIsClass() {
        TypeUtils.init(dummyApi, null);  // Passing null DocletEnvironment for tests that don't need it
        TypeNode node = TypeUtils.createTypeNode("com.example.MyClass", "MyClass", dummyPackage, ElementKind.CLASS);
        assertNotNull(node);
        assertEquals("MyClass", node.getSimpleName());
        assertEquals("com.example.MyClass", node.getQualifiedName());
        // Asserting instance type, depending on your implementation ClassNode extends TypeNode
        assertEquals("com.example.MyClass", node.getQualifiedName());
    }

    @Test
    void createTypeNode_ReturnsInterfaceNode_WhenElementKindIsInterface() {
        TypeUtils.init(dummyApi, null);  // Passing null DocletEnvironment for tests that don't need it
        TypeNode node = TypeUtils.createTypeNode("com.example.MyInterface", "MyInterface", dummyPackage, ElementKind.INTERFACE);
        assertNotNull(node);
        assertEquals("MyInterface", node.getSimpleName());
    }

    @Test
    void createTypeNode_ReturnsNull_WhenElementKindIsUnknown() {
        TypeUtils.init(dummyApi, null);  // Passing null DocletEnvironment for tests that don't need it
        TypeNode node = TypeUtils.createTypeNode("com.example.Unknown", "Unknown", dummyPackage, ElementKind.MODULE);
        assertNull(node);
    }

    @Test
    void getUrl_ReturnsUrl_WhenInputContainsHref() {
        TypeUtils.init(dummyApi, null);  // Passing null DocletEnvironment for tests that don't need it
        String href = "http://example.com";
        String input = "<a href=\"" + href + "\">link</a>";
        String result = TypeUtils.getUrl(input);
        assertEquals(href, result);
    }

    @Test
    void getUrl_ReturnsNull_WhenInputIsNullOrMalformed() {
        TypeUtils.init(dummyApi, null);  // Passing null DocletEnvironment for tests that don't need it
        assertNull(TypeUtils.getUrl(null));
        assertNull(TypeUtils.getUrl("no href here"));
    }

    void setup2() {
        mockApi = mock(Api.class);
        mockEnvironment = mock(DocletEnvironment.class);
        DocTrees dct = mock(DocTrees.class);
        when(mockEnvironment.getDocTrees()).thenReturn(dct);

        simpleName2 = mock(Name.class);
        when(simpleName2.toString()).thenReturn("Foo");
        qualifiedName2 = mock(Name.class);
        when(qualifiedName2.toString()).thenReturn("com.example.Foo");
        packageName2 = mock(Name.class);
        when(packageName2.toString()).thenReturn("com.example");

        TypeUtils.init(mockApi, mockEnvironment);
    }

    @Test
    void nodeFromElement_ReturnsExistingTypeNode() {
        setup2();
        typeElement = mock(TypeElement.class);
        when(typeElement.getQualifiedName()).thenReturn(qualifiedName2);

        TypeNode existingNode = mock(TypeNode.class);
        when(mockApi.getTypeNode("com.example.Foo")).thenReturn(existingNode);

        TypeNode result = TypeUtils.nodeFromElement(typeElement);

        assertSame(existingNode, result);
    }

    @Disabled("WIP")
    @Test
    void nodeFromElement_CreatesTypeNodeForClass() {
        setup2();
        typeElement = mock(TypeElement.class);


        when(typeElement.getQualifiedName()).thenReturn(qualifiedName2);
        when(typeElement.getSimpleName()).thenReturn(simpleName2);
        when(typeElement.getKind()).thenReturn(ElementKind.CLASS);
        packageElement = mock(PackageElement.class);
        when(packageElement.getQualifiedName()).thenReturn(packageName2);

        when(TypeUtils.getEnclosingPackageElement(typeElement)).thenReturn(packageElement);

        packageNode = mock(PackageNode.class);
        when(mockApi.getPackageNode("com.example")).thenReturn(packageNode);

        when(mockApi.getTypeNode("com.example.Foo")).thenReturn(null);

        try (MockedStatic<TypeUtils> utilsStatic = Mockito.mockStatic(TypeUtils.class, Answers.CALLS_REAL_METHODS)) {
            // We bypass call to createTypeNode, and control create behavior for testing
            utilsStatic.when(() -> TypeUtils.createTypeNode("com.example.Foo", "Foo", packageNode, ElementKind.CLASS))
                       .thenCallRealMethod();


            // To avoid eg. infinite recursion in environment etc, we stub methods invoked
            utilsStatic.when(() -> TypeUtils.setTypeOwnership(any(), eq(typeElement))).thenCallRealMethod();
                    
            utilsStatic.when(() -> TypeUtils.setModifiers(any(), any())).thenCallRealMethod();
            utilsStatic.when(() -> TypeUtils.collectAllSupertypes(any(), any())).thenCallRealMethod();
            utilsStatic.when(() -> TypeUtils.findImplementedInterfaces(eq(typeElement), any())).thenCallRealMethod();

            // Run the actual call under test
            TypeNode typeNode = TypeUtils.nodeFromElement(typeElement);

            assertNotNull(typeNode);
            assertEquals("com.example.Foo", typeNode.getQualifiedName());
            verify(mockApi).addType(typeNode);
        }
    }

    @Test
    void setEnumConstants_AddsEnumConstants() {
        setup2();
        EnumTypeNode enumNode = new EnumTypeNode("com.example.Color", "Color", null);
        typeElement = mock(TypeElement.class);
        Element enumConstant = mock(Element.class);
        when(enumConstant.getKind()).thenReturn(ElementKind.ENUM_CONSTANT);

        when(simpleName2.toString()).thenReturn("RED");
        when(enumConstant.getSimpleName()).thenReturn(simpleName2);
        when(typeElement.getEnclosedElements()).thenAnswer(_ ->List.of(enumConstant));

        TypeUtils.setEnumConstants(enumNode, typeElement);

        assertEquals(1, enumNode.getConstants().size());
        assertEquals("RED", enumNode.getConstants().get(0).getSimpleName());
    }

    @Disabled("WIP")
    @Test
    void nodeFromElement_MethodNode_CreatesMethodNodeWithParams() {
        setup2();
        ExecutableElement methodElement = mock(ExecutableElement.class);
        TypeMirror returnType = mock(TypeMirror.class);
        when(returnType.getKind()).thenReturn(TypeKind.DECLARED);
        when(returnType.toString()).thenReturn("java.lang.String");
        when(methodElement.getReturnType()).thenReturn(returnType);
        when(methodElement.getSimpleName()).thenReturn(simpleName2);
        when(methodElement.getKind()).thenReturn(ElementKind.METHOD);
        PackageElement pkg = mock(PackageElement.class);
        when(pkg.getQualifiedName()).thenReturn(qualifiedName2);
        when(TypeUtils.getEnclosingPackageElement(methodElement)).thenReturn(pkg);

        PackageNode packageNode2 = mock(PackageNode.class);
        when(mockApi.getPackageNode("com.example")).thenReturn(packageNode2);

        TypeElement ownerElement = mock(TypeElement.class);
        when(TypeUtils.getEnclosingTypeElement(methodElement)).thenReturn(ownerElement);
        TypeNode ownerType = mock(TypeNode.class);
        when(mockApi.getTypeNode(ownerElement.getQualifiedName().toString())).thenReturn(ownerType);

        when(ownerType.getMethod(any())).thenReturn(null);

        when(methodElement.getParameters()).thenReturn(Collections.emptyList());
        when(methodElement.getModifiers()).thenReturn(Set.of(Modifier.PUBLIC));
        when(methodElement.getThrownTypes()).thenReturn(Collections.emptyList());
        when(methodElement.getAnnotationMirrors()).thenReturn(Collections.emptyList());

        try (MockedStatic<TypeUtils> utilsStatic = Mockito.mockStatic(TypeUtils.class, Answers.CALLS_REAL_METHODS)) {
            utilsStatic.when(() -> TypeUtils.setMethodParams(any(), eq(methodElement))).thenCallRealMethod();
            utilsStatic.when(() -> TypeUtils.setModifiers(any(), any())).thenCallRealMethod();
            utilsStatic.when(() -> TypeUtils.setThrownTypes(any(), any())).thenCallRealMethod();
            utilsStatic.when(() -> TypeUtils.setMethodAnnotations(any(), eq(methodElement))).thenCallRealMethod();
            utilsStatic.when(() -> TypeUtils.setSpecifiedBy(any(), eq(methodElement))).thenCallRealMethod();

            MethodNode methodNode = TypeUtils.nodeFromElement(methodElement);
            assertNotNull(methodNode);
            assertEquals("methodName", methodNode.getSimpleName());
            verify(ownerType).getMethods();
        }
    }

    @Disabled("WIP")
    @Test
    void nodeFromElement_FieldNode_CreatesFieldNode() {
        setup2();
        VariableElement fieldElement = mock(VariableElement.class);

        TypeElement classElement = mock(TypeElement.class);
        when(TypeUtils.getEnclosingTypeElement(fieldElement)).thenReturn(classElement);
        when(classElement.getQualifiedName()).thenReturn(qualifiedName2);
        TypeNode typeNode = spy(new ClassTypeNode("com.example.Foo", "Foo", null));

        when(mockApi.getTypeNode("com.example.Foo")).thenReturn(typeNode);

        Name fieldName = mock(Name.class);
        when(fieldName.toString()).thenReturn("fieldName");
        when(fieldElement.getSimpleName()).thenReturn(fieldName);

        FieldNode existingField = null;
        doReturn(existingField).when(typeNode).getField("fieldName");

        FieldNode resultNode = TypeUtils.nodeFromElement(fieldElement);

        assertNotNull(resultNode);
        assertEquals("fieldName", resultNode.getSimpleName());
    }

    @Test
    void createTextSegment_ProducesExpectedSegment() {
        setup2();
        // Prepare a DocTree mock for TEXT kind
        com.sun.source.doctree.TextTree textTree = mock(com.sun.source.doctree.TextTree.class);
        when(textTree.getKind()).thenReturn(com.sun.source.doctree.DocTree.Kind.TEXT);
        when(textTree.toString()).thenReturn("some text");

        var segment = TypeUtils.createTextSegment(textTree);
        assertEquals(io.github.sandydunlop.markista.model.Text.SegmentKind.TEXT, segment.getKind());
        assertEquals("some text", segment.getText());
    }

    @Test
    void setDocumentation_SetsTextsIfDocCommentPresent() {
        setup2();
        Element element = mock(Element.class);
        docCommentTree = mock(DocCommentTree.class);
        when(mockEnvironment.getDocTrees().getDocCommentTree(element)).thenReturn(docCommentTree);

        // Define lists with exact type: List<? extends DocTree>
        List<? extends DocTree> firstSentence = List.of();
        List<? extends DocTree> body = List.of();
        List<? extends DocTree> fullBody = List.of();

        when(docCommentTree.getFirstSentence()).thenAnswer(_ -> firstSentence);
        when(docCommentTree.getBody()).thenAnswer(_ -> body);
        when(docCommentTree.getFullBody()).thenAnswer(_ -> fullBody);

        var node = new io.github.sandydunlop.markista.model.ClassTypeNode("com.example.Foo", "Foo", null);

        try (var utilsStatic = org.mockito.Mockito.mockStatic(TypeUtils.class, org.mockito.Answers.CALLS_REAL_METHODS)) {
            utilsStatic.when(() -> TypeUtils.createText(firstSentence))
                       .thenReturn(io.github.sandydunlop.markista.model.Text.empty());
            utilsStatic.when(() -> TypeUtils.createText(body))
                       .thenReturn(io.github.sandydunlop.markista.model.Text.empty());
            utilsStatic.when(() -> TypeUtils.createText(fullBody))
                       .thenReturn(io.github.sandydunlop.markista.model.Text.empty());

            TypeUtils.setDocumentation(node, element);

            assertNotNull(node.getFirstSentence());
            assertNotNull(node.getBody());
            assertNotNull(node.getFullBody());
        }
    }

    @Test
    void setAppliedAnnotations() {
        setup2();
        TypeNode typeNode = new TypeNode("com.example.Foo", "Foo", packageNode);


        AnnotationMirror am = mock(AnnotationMirror.class);
        List<? extends AnnotationMirror> annotationMirrors = List.of(am);
        TypeElement ac = mock(TypeElement.class);
        // AnnotatedConstruct ac = mock(AnnotatedConstruct.class);
        when (ac.getAnnotationMirrors()).thenAnswer(_ -> annotationMirrors);
        
        DeclaredType declaredType = mock(DeclaredType.class);
        when (am.getAnnotationType()).thenAnswer(_ -> declaredType);

        Map<? extends ExecutableElement, ? extends AnnotationValue> values = new HashMap<>();
        when (am.getElementValues()).thenAnswer(_ -> values);

        TypeElement declaredElement = mock(TypeElement.class);
        Name declaredName = mock(Name.class);
        when (declaredName.toString()).thenReturn("NewAnnotation");
        when (declaredElement.getQualifiedName()).thenAnswer(_ -> declaredName);
        when (declaredElement.getSimpleName()).thenAnswer(_ -> declaredName);
        when (declaredType.asElement()).thenReturn(declaredElement);
        
        TypeUtils.setAppliedAnnotations(typeNode, ac);
        assertEquals(1, typeNode.getAppliedAnnotations().size());
    }
}
