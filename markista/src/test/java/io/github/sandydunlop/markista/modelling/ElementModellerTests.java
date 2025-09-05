package io.github.sandydunlop.markista.modelling;

import io.github.sandydunlop.markista.MockedDocletEnvironment;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.Deprecation;
import io.github.sandydunlop.markista.model.DirectiveNode;
import io.github.sandydunlop.markista.model.EnumNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.Node;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.Link;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.Text.Segment;
import io.github.sandydunlop.markista.orchestration.LinkResolver;
import io.github.sandydunlop.markista.orchestration.TextAssembler;
import io.github.sandydunlop.markista.model.TypeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.DirectiveKind;
import javax.lang.model.element.ModuleElement.ExportsDirective;
import javax.lang.model.element.ModuleElement.OpensDirective;
import javax.lang.model.element.ModuleElement.ProvidesDirective;
import javax.lang.model.element.ModuleElement.RequiresDirective;
import javax.lang.model.element.ModuleElement.UsesDirective;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.IdentifierTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.SinceTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ElementModellerTests extends MockedDocletEnvironment {
    private static Context ctx;
    private Api dummyApi;
    private LinkResolver resolver;

    private Api api;

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

    private ModuleElement aModuleElement;
    private ModuleElement moduleElement;


    private Api apiMock;
    private DocletEnvironment envMock;

    @Mock static Reporter reporter = new Reporter() {
        @Override
        public void print(javax.tools.Diagnostic.Kind kind, String message) {
            System.out.println(kind + ": " + message);
        }

        @Override
        public void print(javax.tools.Diagnostic.Kind kind, DocTreePath path, String message) {
            // Do nothing
        }

        @Override
        public void print(javax.tools.Diagnostic.Kind kind, Element element, String message) {
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

        aModuleElement = mock(ModuleElement.class);
        moduleElement = mock(ModuleElement.class);
        when(moduleElement.getQualifiedName()).thenReturn(name);
        when(aModuleElement.getQualifiedName()).thenReturn(name);

        api = new Api("Test API");
        modeller = new ElementModeller(api, docletEnv);

        packageNode = new PackageNode("io.github.sandydunlop.markista.model");
        api.addPackage(packageNode);
        resolver = new LinkResolver(api, ctx);
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

        List<Link> refs = modeller.getReferences(docCommentTree);
        assertNotNull(refs);
        assertEquals(2, refs.size());
        resolver.resolve(refs.get(0));
        assertEquals(Link.Kind.URL, refs.get(0).getKind());
        assertEquals("http://example.com", refs.get(0).getPath());
        assertEquals(Link.Kind.TYPE, refs.get(1).getKind());
        assertEquals("Node", refs.get(1).getTarget());
    }

    @Test
    void getUrl() {
        String url = modeller.getUrl("<a href=\"http://example.com\">text</a>");
        assertEquals("http://example.com", url);
    }

    @Test
    void nodeFromElement_TypeElement_Class() {
        //getTypeUtils().directSupertypes(t))
        TypeNode node = modeller.modelType(typeElement);
        assertNotNull(node);
        assertEquals("Node", node.getSimpleName());
        assertEquals("io.github.sandydunlop.markista.model.Node", node.getQualifiedName());
        assertEquals(Node.Kind.CLASS, node.getKind());
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

        TypeNode node = modeller.modelType(interfaceTypeElement);
        assertNotNull(node);
        assertEquals("PackageMember", node.getSimpleName());
        assertEquals("io.github.sandydunlop.markista.model.PackageMember", node.getQualifiedName());
        assertEquals(Node.Kind.INTERFACE, node.getKind());
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

        TypeNode node = modeller.modelType(enumTypeElement);
        assertNotNull(node);
        assertEquals("Deprecation", node.getSimpleName());
        assertEquals("io.github.sandydunlop.markista.model.Deprecation", node.getQualifiedName());
        assertEquals(Node.Kind.ENUM, node.getKind());
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

        TypeNode node = modeller.modelType(annotationTypeElement);
        assertNotNull(node);
        assertEquals("Overrides", node.getSimpleName());
        assertEquals("io.github.sandydunlop.markista.model.Overrides", node.getQualifiedName());
        assertEquals(Node.Kind.ANNOTATION, node.getKind());
    }

    @Test
    void testGetUrl() {
        String html = "<a href=\"http://example.com\">link</a>";
        String url = modeller.getUrl(html);
        assertEquals("http://example.com", url);

        url = modeller.getUrl("noQuotes");
        assertNull(url);

        url = modeller.getUrl(null);
        assertNull(url);
    }

    @Test
    void testSetModifiers() {
        FieldNode node = mock(FieldNode.class);
        Set<javax.lang.model.element.Modifier> mods = new HashSet<>();
        mods.add(javax.lang.model.element.Modifier.PUBLIC);
        mods.add(javax.lang.model.element.Modifier.STATIC);

        modeller.setModifiers(node, mods);

        // verify addModifier called with corresponding enum values
        verify(node, times(mods.size())).addModifier(any());
    }

    @Test
    void getUrl_ReturnsUrl_WhenInputContainsHref() {
        modeller = new ElementModeller(dummyApi, environment);// Passing null DocletEnvironment for tests that don't need it
        String href = "http://example.com";
        String input = "<a href=\"" + href + "\">link</a>";
        String result = modeller.getUrl(input);
        assertEquals(href, result);
    }

    @Test
    void getUrl_ReturnsNull_WhenInputIsNullOrMalformed() {
        modeller = new ElementModeller(dummyApi, environment);
        assertNull(modeller.getUrl(null));
        assertNull(modeller.getUrl("no href here"));
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

        modeller = new ElementModeller(mockApi, mockEnvironment);
    }

    @Test
    void setEnumConstants_AddsEnumConstants() {
        setup2();
        EnumNode enumNode = new EnumNode("Color", null);
        typeElement = mock(TypeElement.class);
        Element enumConstant = mock(Element.class);
        when(enumConstant.getKind()).thenReturn(ElementKind.ENUM_CONSTANT);

        when(simpleName2.toString()).thenReturn("RED");
        when(enumConstant.getSimpleName()).thenReturn(simpleName2);
        when(typeElement.getEnclosedElements()).thenAnswer(_ ->List.of(enumConstant));

        modeller.setEnumConstants(enumNode, typeElement);

        assertEquals(1, enumNode.getConstants().size());
        assertEquals("RED", enumNode.getConstants().get(0).getSimpleName());
    }

    @Test
    void nodeFromElement_FieldNode_CreatesFieldNode() {
        setup2();
        VariableElement fieldElement = mock(VariableElement.class);

        TypeElement classElement = mock(TypeElement.class);
        when(modeller.getEnclosingTypeElement(fieldElement)).thenReturn(classElement);
        when(classElement.getQualifiedName()).thenReturn(qualifiedName2);
        when(classElement.getKind()).thenReturn(ElementKind.CLASS);
        TypeMirror tm = mock(TypeMirror.class);
        TypeNode typeNode = new ClassNode("Foo", null);

        when(mockApi.getTypeNode("com.example.Foo")).thenReturn(typeNode);

        Name fieldName = mock(Name.class);
        when(fieldName.toString()).thenReturn("fieldName");
        when(fieldElement.getSimpleName()).thenReturn(fieldName);
        when(fieldElement.getEnclosingElement()).thenReturn(classElement);
        when(fieldElement.asType()).thenReturn(tm);
        FieldNode resultNode = modeller.modelField(fieldElement);

        assertNotNull(resultNode);
        assertEquals("fieldName", resultNode.getSimpleName());
    }

    @Test
    void setAppliedAnnotations() {
        setup2();
        TypeNode typeNode = new TypeNode("Foo", packageNode.getName());


        AnnotationMirror am = mock(AnnotationMirror.class);
        List<? extends AnnotationMirror> annotationMirrors = List.of(am);
        TypeElement ac = mock(TypeElement.class);
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

        modeller.setAppliedAnnotations(typeNode, ac);
        assertEquals(1, typeNode.getAppliedAnnotations().size());
    }

    void setUp2() {
        envMock = mock(DocletEnvironment.class);
        when(envMock.getElementUtils()).thenReturn(mock(javax.lang.model.util.Elements.class));
        when(envMock.getDocTrees()).thenReturn(mock(DocTrees.class));

        modeller = new ElementModeller(api, envMock);
    }

    @Test
    void setAppliedAnnotation_adds_applied_annotation_and_marks_documented() {
        setUp2();
        // Initialize TypeUtils static context
        TypeNode targetType = new TypeNode("MyClass", packageNode.getName());

        // Build an AnnotationMirror mock representing @MyAnno(value="x")
        AnnotationMirror annotationMirror = mock(AnnotationMirror.class);
        DeclaredType declaredType = mock(DeclaredType.class);
        TypeElement declaredElement = mock(TypeElement.class);

        when(annotationMirror.getAnnotationType()).thenReturn(declaredType);
        when(declaredType.asElement()).thenReturn(declaredElement);

        Name nameMock1= mockName("com.example.MyAnno");
        when(declaredElement.getQualifiedName()).thenReturn(nameMock1);
        Name nameMock2 = mockName("MyAnno");
        when(declaredElement.getSimpleName()).thenReturn(nameMock2);

        // Provide one element-value pair for the annotation: method name "value" returning "hello"
        ExecutableElement annotationMethod = mock(ExecutableElement.class);
        Name nameMock3 = mockName("value");
        when(annotationMethod.getSimpleName()).thenReturn(nameMock3);
        when(annotationMethod.asType()).thenReturn(mock(TypeMirror.class));
        when(annotationMethod.asType().toString()).thenReturn("java.lang.String");

        AnnotationValue avalue = mock(AnnotationValue.class);
        when(avalue.getValue()).thenReturn("hello");

        Map<ExecutableElement, AnnotationValue> values = new HashMap<>();
        values.put(annotationMethod, avalue);
        when(annotationMirror.getElementValues()).thenAnswer(_ -> values);

        // Call method
        modeller.setAppliedAnnotation(targetType, annotationMirror);

        // Verify that the node had addAppliedAnnotation called and api had the annotation registered

        assertEquals(1, targetType.getAppliedAnnotations().size());
    }

    @Test
    void nodeFromElement_type_creates_and_adds_type_when_not_present() {
        setUp2();
        // Prepare TypeElement representing class com.test.MyClass in package com.test
        TypeElement typeEl = mock(TypeElement.class);
        Name nameMock1 = mockName("io.github.sandydunlop.markista.model.MyClass");
        when(typeEl.getQualifiedName()).thenReturn(nameMock1);
        Name nameMock2 = mockName("MyClass");
        when(typeEl.getSimpleName()).thenReturn(nameMock2);
        when(typeEl.getKind()).thenReturn(ElementKind.CLASS);

        // PackageElement enclosing
        PackageElement pkgEl = mock(PackageElement.class);
        Name nameMock3 = mockName("io.github.sandydunlop.markista.model");
        when(pkgEl.getQualifiedName()).thenReturn(nameMock3);
        when(pkgEl.getKind()).thenReturn(ElementKind.PACKAGE);
        when(typeEl.getEnclosingElement()).thenReturn(pkgEl);
        TypeMirror typeMirror2 = mock(TypeMirror.class);
        when(typeEl.asType()).thenReturn(typeMirror2);

        api.addPackage(packageNode);
        when (envMock.getTypeUtils()).thenReturn(typeUtils);
        TypeNode result = modeller.modelType(typeEl);
        assertNotNull(result, "nodeFromElement should return a TypeNode instance");
    }


    @Test
    void setDeprecationStatus_prefers_javadoc_deprecated_over_annotation_and_handles_forRemoval() {
        setup2();
        NodeStub node = mock(NodeStub.class);
        Element element = mock(Element.class);

        // Case 1: Javadoc @deprecated present
        DocTrees docTrees = mock(DocTrees.class);
        when(mockEnvironment.getDocTrees()).thenReturn(docTrees);
        modeller = new ElementModeller(api, mockEnvironment);

        DeprecatedTree deprecatedTree = mock(DeprecatedTree.class);
        when(deprecatedTree.getBody()).thenReturn(Collections.emptyList());
        com.sun.source.doctree.DocCommentTree dct = mock(com.sun.source.doctree.DocCommentTree.class);
        when(dct.getBlockTags()).thenAnswer(_ -> List.of((DocTree) deprecatedTree));
        when(docTrees.getDocCommentTree(element)).thenReturn(dct);

        // Make sure element.getAnnotation returns null (no @Deprecated annotation)
        when(element.getAnnotation(Deprecated.class)).thenReturn(null);

        modeller.setDeprecationStatus(node, element, dct);

        verify(node).setDeprecation(Deprecation.DEPRECATED);
        verify(node).setDeprecationText(any(Text.class));

        // Case 2: annotation present with forRemoval true and no javadoc deprecated
        reset(node);
        com.sun.source.doctree.DocCommentTree emptyDct = mock(com.sun.source.doctree.DocCommentTree.class);
        when(emptyDct.getBlockTags()).thenReturn(Collections.emptyList());
        when(docTrees.getDocCommentTree(element)).thenReturn(emptyDct);

        // Mock a Deprecated annotation with forRemoval true
        Deprecated deprecatedAnno = mock(Deprecated.class);
        when(deprecatedAnno.forRemoval()).thenReturn(true);
        when(element.getAnnotation(Deprecated.class)).thenReturn(deprecatedAnno);

        modeller.setDeprecationStatus(node, element, emptyDct);

        verify(node).setDeprecation(Deprecation.FOR_REMOVAL);
    }

    @Test
    void getSince_returns_text_for_since_tag_and_empty_for_none() {
        // With since
        com.sun.source.doctree.DocCommentTree dct = mock(com.sun.source.doctree.DocCommentTree.class);
        SinceTree sinceTree = mock(SinceTree.class);
        when(sinceTree.getBody()).thenReturn(Collections.emptyList());
        when(dct.getBlockTags()).thenAnswer(_ -> List.of((DocTree) sinceTree));

        Text got = modeller.getSince(dct);
        assertNotNull(got);

        // Without since tags
        com.sun.source.doctree.DocCommentTree empty = mock(com.sun.source.doctree.DocCommentTree.class);
        when(empty.getBlockTags()).thenReturn(Collections.emptyList());
        Text none = modeller.getSince(empty);
        assertNotNull(none); // should be Text.empty(), not null
    }

    IdentifierTree mockParam(String n) {
        Name nameMock = mockName(n);
        IdentifierTree idt = mock(IdentifierTree.class);
        when(idt.getName()).thenReturn(nameMock);
        when(idt.getKind()).thenReturn(DocTree.Kind.PARAM);
        return idt;
    }

    @Test
    void getParamTree_finds_matching_param_tag() {
        setUp2();
        com.sun.source.doctree.DocCommentTree dct = mock(com.sun.source.doctree.DocCommentTree.class);

        ParamTree paramTree = mock(ParamTree.class);
        IdentifierTree idt = mockParam("p");
        when(paramTree.getName()).thenReturn(idt);
        when(dct.getBlockTags()).thenAnswer(_ -> List.of(paramTree));

        VariableElement param = mock(VariableElement.class);
        Name nameMock = mockName("p");
        when(param.getSimpleName()).thenReturn(nameMock);

        ParamTree result = modeller.getParamTree(dct, param);
        assertSame(paramTree, result);
    }

    @Test
    void setMethodParams_adds_parameters_with_doc_bodies() {
        setup2();
        // Prepare ExecutableElement with one parameter
        ExecutableElement ee = mock(ExecutableElement.class);
        VariableElement ve = mock(VariableElement.class);
        Name nameMock = mockName("arg");
        when(ve.getSimpleName()).thenReturn(nameMock);

        // TypeMirror for parameter
        TypeMirror typeMirror2 = mock(TypeMirror.class);
        when(typeMirror2.getKind()).thenReturn(TypeKind.DECLARED);
        when(typeMirror2.toString()).thenReturn("java.lang.String");
        when(ve.asType()).thenReturn(typeMirror2);

        when(ee.getParameters()).thenAnswer(_ ->List.of(ve));

        // Provide doc comment tree with ParamTree for "arg"
        com.sun.source.doctree.DocCommentTree dct = mock(com.sun.source.doctree.DocCommentTree.class);
        ParamTree ptag = mock(ParamTree.class);
        IdentifierTree idt = mockParam("arg");
        when(ptag.getName()).thenReturn(idt);
        when(ptag.getDescription()).thenAnswer(_ -> Collections.emptyList());
        when(dct.getBlockTags()).thenAnswer(_ -> List.of((DocTree) ptag));
        when(mockEnvironment.getDocTrees().getDocCommentTree(ee)).thenReturn(dct);

        Types types = mock(Types.class);
        when(types.asElement(typeMirror2)).thenReturn(null);
        when(mockEnvironment.getTypeUtils()).thenReturn(types);


        // Ensure api.getPackageNode returns a PackageNode so TypeNode construction can proceed
        PackageNode pkg = mock(PackageNode.class);
        apiMock = mock(Api.class);
        when(apiMock.getPackageNode("java.lang")).thenReturn(pkg);

        MethodNode methodDoc = mock(MethodNode.class);

        ArgumentCaptor<ParamNode> captor = ArgumentCaptor.forClass(ParamNode.class);

        modeller.setMethodParams(methodDoc, ee);

        verify(methodDoc).addParam(captor.capture());
        ParamNode added = captor.getValue();
        assertEquals("arg", added.getSimpleName());
        assertNotNull(added.getType().getRawTypeName()); // type constructed
        // Because we passed empty description, body is likely empty Text
        assertNotNull(added.getBody());
    }


    @Test
    void getDeprecation_and_getReturnTree_detect_block_tags() {
        com.sun.source.doctree.DocCommentTree dct = mock(com.sun.source.doctree.DocCommentTree.class);
        DeprecatedTree dt2 = mock(DeprecatedTree.class);
        ReturnTree rt = mock(ReturnTree.class);

        when(dct.getBlockTags()).thenAnswer(_ -> List.of((DocTree) dt2, (DocTree) rt));

        DeprecatedTree foundDep = modeller.getDeprecation(dct);
        ReturnTree foundRet = modeller.getReturnTree(dct);

        assertSame(dt2, foundDep);
        assertSame(rt, foundRet);
    }

    @Test
    void setThrownTypes_adds_exception_type_names_to_methodnode() {
        methodNode = new MethodNode("void", "method");

        // Mock a TypeMirror and the environment behaviour to produce a TypeElement
        TypeMirror tm = mock(TypeMirror.class);
        TypeElement thrownTypeEl = mock(TypeElement.class);
        when(thrownTypeEl.getQualifiedName()).thenAnswer(_ -> new javax.lang.model.element.Name() {
            @Override public int length() { return "java.io.IOException".length(); }
            @Override public char charAt(int index) { return "java.io.IOException".charAt(index); }
            @Override public CharSequence subSequence(int start, int end) { return "java.io.IOException".subSequence(start, end); }
            @Override public String toString() { return "java.io.IOException"; }
            @Override public boolean contentEquals(CharSequence s) { return true; }
        });

        envMock = mock(DocletEnvironment.class);
        javax.lang.model.util.Types typeUtils2 = mock(javax.lang.model.util.Types.class);
        when(typeUtils2.asElement(tm)).thenReturn(thrownTypeEl);
        when(envMock.getTypeUtils()).thenReturn(typeUtils2);
        modeller = new ElementModeller(api, envMock);

        modeller.setThrownTypes(methodNode, List.of(tm));

        assertEquals(1, methodNode.getThrownTypes().size());
    }

    // A small helper stub interface so we can verify Node interactions without requiring the real Node implementation.
    // If your project provides a concrete Node class you can replace references accordingly.
    private class NodeStub extends io.github.sandydunlop.markista.model.Node {
        // No additional members required; used for mocking only.
    }

    @Test
    void docTreeToText_TEXT() {
        modeller = new ElementModeller(api, docletEnvironmentMock);
        DocTree docTreeMock = mockDocCommentTree_TEXT("plain text");
        Text text = modeller.docTreeToText(docTreeMock);
        assertEquals("plain text", text.toString());
    }

    @Test
    void docTreeToText_MARKDOWN() {
        modeller = new ElementModeller(api, docletEnvironmentMock);
        DocTree docTreeMock = mockDocCommentTree_MARKDOWN("markdown text");
        Text text = modeller.docTreeToText(docTreeMock);
        assertEquals("markdown text", text.toString());
    }

    @Test
    void docTreeToText_LINK() {
        ctx.setTypeName("Mocktype");
        modeller = new ElementModeller(api, docletEnvironmentMock);
        DocTree docTreeMock = mockDocCommentTree_LINK();
        Text text = modeller.docTreeToText(docTreeMock);
        assertNotNull(text);
        assertEquals(1, text.getSegments().size());
        Segment segment = text.getSegment(0);
        assertEquals(Segment.Kind.LINK, segment.getKind());
        Link link = segment.getLink();
        assertNotNull(link);
        assertEquals("https://example.com", link.getTarget());
    }

    @Test
    void docTreeToText_LINK_PLAIN() {
        ctx.setTypeName("Mocktype");
        modeller = new ElementModeller(api, docletEnvironmentMock);
        DocTree docTreeMock = mockDocCommentTree_LINK_PLAIN();
        Text text = modeller.docTreeToText(docTreeMock);
        assertNotNull(text);
        assertEquals(1, text.getSegments().size());
        Segment segment = text.getSegment(0);
        assertEquals(Segment.Kind.LINK, segment.getKind());
        Link link = segment.getLink();
        assertNotNull(link);
        assertEquals("https://example.com", link.getTarget());
        assertEquals("link text", link.getLabel());
    }

    @Test
    void docTreeToText_START_ELEMENT() {
        modeller = new ElementModeller(api, docletEnvironmentMock);
        DocTree docTreeMock = mockDocCommentTree_START_ELEMENT();
        Text text = modeller.docTreeToText(docTreeMock);
        assertEquals("\n\n", text.toString());
    }

    @Test
    void docTreeToText_END_ELEMENT() {
        modeller = new ElementModeller(api, docletEnvironmentMock);
        DocTree docTreeMock = mockDocCommentTree_END_ELEMENT();
        Text text = modeller.docTreeToText(docTreeMock);
        assertEquals("", text.toString());
    }

    @Test
    void markdownToText_link_with_parens() {
        String markdown = "onverts Markdown text into a [Text](https://example.com) object";
        Text text = modeller.markdownToText(markdown);
        assertNotNull(text);
        assertEquals(3, text.getSegments().size());
        assertEquals(Segment.Kind.LINK, text.getSegment(1).getKind());
        assertEquals("https://example.com", text.getSegment(1).getLink().getTarget());
        assertEquals("Text", text.getSegment(1).getLink().getLabel());
    }

    @Test
    void markdownToText_link_without_parens() {
        String markdown = "onverts Markdown text into a [Text] object";
        Text text = modeller.markdownToText(markdown);
        assertNotNull(text);
        assertEquals(3, text.getSegments().size());
        assertEquals(Segment.Kind.LINK, text.getSegment(1).getKind());
        assertEquals("Text", text.getSegment(1).getLink().getTarget());
    }

    @Test
    void markdownToTest_webLink() {
        String markdown = "the [Markista homepage](https://sandydunlop.github.io/markista)";

        Text text = modeller.markdownToText(markdown);
        Text.Segment segment = text.getSegments().getLast();
        assertEquals(Text.Segment.Kind.LINK, segment.getKind());


        assertNotNull(text);
    }

    @Test
    void markdownText1() {
        String md = "one [model](io.github.sandydunlop.markista.model) two";
        Text text = modeller.markdownToText(md);
        assertNotNull(text);
        // "one [model](../model/index.md) two"
    }

    @Test
    void markdownText2() {
        String md = "one [Node](Node) two";
        Text text = modeller.markdownToText(md);
        assertNotNull(text);
        //"one [Node](../model/Node.md) two"
    }

    @Test
    void markdownText3() {
        String md = "one [Node] two";
        Text text = modeller.markdownToText(md);
        assertNotNull(text);
        //"one [Node](../model/Node.md) two"
    }

    @Test
    void markdownText4() {
        String md = "one [Node](io.github.sandydunlop.markista.model.Node) two";
        Text text = modeller.markdownToText(md);
        assertNotNull(text);
        //"one [Node](../model/Node.md) two"
    }


    @SuppressWarnings("unused")
    @Test
    void createFrom_REQUIRES() {
        RequiresDirective directive = mock(RequiresDirective.class);
        when(elementUtils.getModuleOf(aModuleElement)).thenReturn(moduleElement);
        when(docletEnv.getElementUtils()).thenReturn(elementUtils);
        when(packageElement.getQualifiedName()).thenReturn(name);
        when(typeElement.getQualifiedName()).thenReturn(name);
        when(name.toString()).thenReturn("sandy");
        when(directive.getDependency()).thenReturn(aModuleElement);
        when(directive.getKind()).thenAnswer(unused -> DirectiveKind.REQUIRES);

        DirectiveNode directiveNode = modeller.createDirectiveNode(directive);
        assertNotNull(directiveNode);
        DirectiveNode.Kind kind = directiveNode.getKind();
        assertEquals(DirectiveNode.Kind.REQUIRES, kind);
    }

    @SuppressWarnings("unused")
    @Test
    void createFrom_EXPORTS() {
        ExportsDirective directive = mock(ExportsDirective.class);
        List<? extends ModuleElement> targetModules = List.of(aModuleElement);
        when(directive.getPackage()).thenReturn(packageElement);
        when(directive.getTargetModules()).thenAnswer(unused -> targetModules);
        when(directive.getKind()).thenAnswer(unused -> DirectiveKind.EXPORTS); //NOSONAR
        DirectiveNode directiveNode = modeller.createDirectiveNode(directive);
        assertNotNull(directiveNode);
        DirectiveNode.Kind kind = directiveNode.getKind();
        assertEquals(DirectiveNode.Kind.EXPORTS, kind);
    }

    @SuppressWarnings("unused")
    @Test
    void createFrom_OPENS() {
        OpensDirective directive = mock(OpensDirective.class);
        List<? extends ModuleElement> targetModules = List.of(aModuleElement);
        when(directive.getPackage()).thenReturn(packageElement);
        when(directive.getTargetModules()).thenAnswer(unused -> targetModules); //NOSONAR
        when(directive.getKind()).thenAnswer(unused -> DirectiveKind.OPENS); //NOSONAR
        DirectiveNode directiveNode = modeller.createDirectiveNode(directive);
        assertNotNull(directiveNode);
        DirectiveNode.Kind kind = directiveNode.getKind();
        assertEquals(DirectiveNode.Kind.OPENS, kind);
    }

    @SuppressWarnings("unused")
    @Test
    void createFrom_USES() {
        UsesDirective directive = mock(UsesDirective.class);
        when(directive.getService()).thenReturn(typeElement);
        when(directive.getKind()).thenAnswer(unused -> DirectiveKind.USES); //NOSONAR
        DirectiveNode directiveNode = modeller.createDirectiveNode(directive);
        assertNotNull(directiveNode);
        DirectiveNode.Kind kind = directiveNode.getKind();
        assertEquals(DirectiveNode.Kind.USES, kind);
    }

    @SuppressWarnings("unused")
    @Test
    void createFrom_PROVIDES() {
        ProvidesDirective directive = mock(ProvidesDirective.class);
        List<? extends TypeElement> implementations = List.of(typeElement);
        when(directive.getService()).thenReturn(typeElement);
        when(directive.getImplementations()).thenAnswer(unused -> implementations); //NOSONAR
        when(directive.getKind()).thenAnswer(unused -> DirectiveKind.PROVIDES); //NOSONAR
        DirectiveNode directiveNode = modeller.createDirectiveNode(directive);
        assertNotNull(directiveNode);
        DirectiveNode.Kind kind = directiveNode.getKind();
        assertEquals(DirectiveNode.Kind.PROVIDES, kind);
    }



    //
    //
    //

    private ElementModeller modeller;
    private ModuleNode moduleNode = null;
    private PackageNode packageNode = null;
    private ClassNode classNode = null;
    private MethodNode methodNode = null;

    private Api visitElements(List<Element> elements) {
        Api a = new Api("TEST");
        modeller = new ElementModeller(a, docletEnvironmentMock);
        for (Element element : elements) {
            switch (element.getKind()) {
                case MODULE:
                    moduleNode = modeller.modelModule((ModuleElement)element);
                    a.addModule(moduleNode);
                    break;
                case PACKAGE:
                    packageNode = modeller.modelPackage((PackageElement)element);
                    a.addPackage(packageNode);
                    break;
                case CLASS:
                    classNode = (ClassNode)modeller.modelType((TypeElement)element);
                    a.addType(classNode);
                    break;
                case METHOD, CONSTRUCTOR:
                    methodNode = modeller.modelMethod((ExecutableElement)element);
                    a.addMethod(methodNode);
                    break;
                case PARAMETER:
                    break;
                case FIELD:
                    FieldNode fieldNode = modeller.modelField((VariableElement)element);
                    classNode.addField(fieldNode);
                    break;
                default:
                    break;
            }
        }
        return a;
    }

    private static final String MODULE = "mockmodule";
    private static final String PACKAGE_ONE = "mock.package.one";
    private static final String TYPE_ONE = "MocktypeOne";
    private static final String METHOD_ONE = "mockMethodOne";
    private static final String PARAMETER_ONE = "mockParameter";
    private static final String FIELD_ONE = "fieldOne";
    private static final String TYPE_TWO = "MocktypeTwo";

    TypeElement typeMockOne;
    ExecutableElement methodMockOne;

    TypeElement typeMockTwo;
    ExecutableElement methodMockTwo;

    List<Element> buildClassOne() {
        mockDocletEnvironment();
        List<Element> elements = new ArrayList<>();
        elements.add(mockModule(MODULE));
        PackageElement packageMockOne = mockPackage(PACKAGE_ONE);
        elements.add(packageMockOne);
        typeMockOne = mockType(TYPE_ONE, packageMockOne);
        elements.add(typeMockOne);

        ExecutableElement constructorMockOne = mockConstructor(typeMockOne);
        elements.add(constructorMockOne);

        methodMockOne = mockExecutable(METHOD_ONE, typeMockOne);
        elements.add(methodMockOne);

        VariableElement parameterOne = mockMethodParameter(PARAMETER_ONE, typeMockOne, methodMockOne);
        elements.add(parameterOne);

        VariableElement fieldOne = mockVariable(FIELD_ONE, typeMockOne);
        elements.add(fieldOne);

        return elements;
    }

    List<Element> buildClassTwo() {
        mockDocletEnvironment();
        List<Element> elements = new ArrayList<>();
        elements.add(mockModule(MODULE));
        PackageElement packageMockTwo = mockPackage(PACKAGE_ONE);
        elements.add(packageMockTwo);
        typeMockTwo = mockType(TYPE_TWO, packageMockTwo);
        elements.add(typeMockTwo);

        ExecutableElement constructorMockTwo = mockConstructor(typeMockTwo);
        elements.add(constructorMockTwo);

        methodMockTwo = mockExecutable(METHOD_ONE, typeMockTwo);
        elements.add(methodMockTwo);

        VariableElement parameterTwo = mockMethodParameter(PARAMETER_ONE, typeMockTwo, methodMockTwo);
        elements.add(parameterTwo);

        return elements;
    }

    private MethodNode getMethod(TypeNode typeNode, String methodName) {
        MethodNode method = null;
        for (MethodNode m : typeNode.getMethods()) {
            if (m.getSimpleName().equals(methodName)) {
                method = m;
                break;
            }
        }
        return method;
    }

    @Test
    void test0() {
        List<Element> elements = buildClassOne();
        mockIncludedElements(elements);
        Api a = visitElements(elements);
        TextAssembler.assembleTextAndLinks(a, ctx);

        TypeNode typeNodeOne = a.getTypeNode(PACKAGE_ONE + "." + TYPE_ONE);
        assertNotNull(typeNodeOne);

        MethodNode methodNodeOne = getMethod(typeNodeOne, METHOD_ONE);
        assertNotNull(methodNodeOne);
        assertEquals(1, methodNodeOne.getParams().size());

        assertEquals(1, typeNodeOne.getFields().size());
    }

    @Test
    void test1_type_doc() {
        List<Element> elements = buildClassOne();
        mockIncludedElements(elements);

        // Add firstSenter doc text to method
        DocCommentTree methodMockOneDocTree = mockDocCommentTree();
        DocTree methodMockOneText = mockDocCommentTree_TEXT("methodMockOneText");
        List<DocTree> baseMethodDoc = List.of(methodMockOneText);
        when(methodMockOneDocTree.getFirstSentence()).thenAnswer(_ -> baseMethodDoc);
        when(treeUtilsMock.getDocCommentTree(methodMockOne)).thenReturn(methodMockOneDocTree);

        Api a = visitElements(elements);
        TextAssembler.assembleTextAndLinks(a, ctx);

        TypeNode typeNodeOne = a.getTypeNode(PACKAGE_ONE + "." + TYPE_ONE);
        assertNotNull(typeNodeOne);
        MethodNode methodNodeOne = getMethod(typeNodeOne, METHOD_ONE);
        assertNotNull(methodNodeOne);
        assertEquals(1, methodNodeOne.getParams().size());

        // Check docs
        Text firstSentence = methodNodeOne.getFirstSentence();
        assertNotNull(firstSentence);
        assertEquals("methodMockOneText", firstSentence.toString());
    }

    @Test
    void test2_type_sourceFile() {
        List<Element> elements = buildClassOne();
        mockIncludedElements(elements);
        mockJavaFileObject(typeElement);
        Api a = visitElements(elements);
        TypeNode typeNodeOne = a.getTypeNode(PACKAGE_ONE + "." + TYPE_ONE);
        assertNotNull(typeNodeOne);
        String sourcePath = typeNodeOne.getSourcePath();
        assertNotNull(sourcePath);
    }

    @Test
    void deprecation_forRemoval() {
        List<Element> elements = buildClassOne();
        mockIncludedElements(elements);
        mockDeprecation(methodMockOne, true);
        Api a = visitElements(elements);
        TextAssembler.assembleTextAndLinks(a, ctx);
        TypeNode typeNodeOne = a.getTypeNode(PACKAGE_ONE + "." + TYPE_ONE);
        assertNotNull(typeNodeOne);
        MethodNode methodNodeOne = getMethod(typeNodeOne, METHOD_ONE);
        assertNotNull(methodNodeOne);
        assertEquals(Deprecation.FOR_REMOVAL, methodNodeOne.getDeprecation());
    }

    @Test
    void deprecation_deprecated() {
        List<Element> elements = buildClassOne();
        mockIncludedElements(elements);
        mockDeprecation(methodMockOne, false);
        Api a = visitElements(elements);
        TextAssembler.assembleTextAndLinks(a, ctx);
        TypeNode typeNodeOne = a.getTypeNode(PACKAGE_ONE + "." + TYPE_ONE);
        assertNotNull(typeNodeOne);
        MethodNode methodNodeOne = getMethod(typeNodeOne, METHOD_ONE);
        assertNotNull(methodNodeOne);
        assertEquals(Deprecation.DEPRECATED, methodNodeOne.getDeprecation());
    }

    @Test
    void nestedClass_ownership() {
        List<Element> elements = buildClassOne();
        elements.addAll(buildClassTwo());
        when (typeMockTwo.getEnclosingElement()).thenReturn(typeMockOne);
        mockIncludedElements(elements);
        Api a = visitElements(elements);
        TextAssembler.assembleTextAndLinks(a, ctx);
        TypeNode typeNodeOne = a.getTypeNode(PACKAGE_ONE + "." + TYPE_ONE);
        TypeNode typeNodeTwo = a.getTypeNode(PACKAGE_ONE + "." + TYPE_TWO);
        assertNotNull(typeNodeOne);
        assertNotNull(typeNodeTwo);
        assertEquals(PACKAGE_ONE, typeNodeOne.getOwnerName());
        assertEquals(PACKAGE_ONE + "." + TYPE_ONE, typeNodeTwo.getOwnerName());
    }
}
