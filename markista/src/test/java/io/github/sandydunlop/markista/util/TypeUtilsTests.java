package io.github.sandydunlop.markista.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;
import com.sun.source.doctree.StartElementTree;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.OverriddenMethodNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.Reference;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.TypeNode;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;


class TypeUtilsTests {
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
    }

    @BeforeEach
    void init() {
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

        api = new Api();
        TypeUtils.init(api, docletEnv);

        packageNode = new PackageNode("io.github.sandydunlop.markista.model");
        api.addPackage(packageNode);
    }

    @Test
    void addConstantFieldValuesReference() {
        ClassNode classNode2 = new ClassNode("io.github.sandydunlop.markista.model.TypeNode", "Node", packageNode);
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

        ClassNode classNode1 = new ClassNode("io.github.sandydunlop.markista.model.Node", "Node", packageNode);
        api.addType(classNode1);
        TypeNode returnType1 = new TypeNode("int","int", null);
        MethodNode methodNode1 = new MethodNode(returnType1, "length");
        methodNode1.setOwner(classNode1);
        classNode1.addMethod(methodNode1);

        ClassNode classNode2 = new ClassNode("io.github.sandydunlop.markista.model.TypeNode", "Node", packageNode);
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

        ClassNode classNode = new ClassNode("io.github.sandydunlop.markista.model.Node", "Node", packageNode);
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
        assertEquals("Node", refs.get(1).getName());
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
        ClassNode classNode = new ClassNode("io.github.sandydunlop.markista.model.Node", "Node", packageNode);
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
        ClassNode classNode = new ClassNode("io.github.sandydunlop.markista.model.Node", "Node", packageNode);
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
        ClassNode classNode = new ClassNode("io.github.sandydunlop.markista.model.Node", "Node", packageNode);
        api.addType(classNode);
        TypeUtils.setDocumentation(classNode, typeElement);
        assertEquals("berry", Markdown.formatText(classNode.getFirstSentence()));
        assertEquals("berry", Markdown.formatText(classNode.getBody()));
        assertEquals("berry", Markdown.formatText(classNode.getFullBody()));
    }
}
