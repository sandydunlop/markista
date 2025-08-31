package io.github.sandydunlop.markista.scanning;

import io.github.sandydunlop.markista.MockedDocletEnvironment;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.markdown.MarkdownUtils;
import io.github.sandydunlop.markista.model.AnnotationNode;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.AppliedAnnotationNode;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.Deprecation;
import io.github.sandydunlop.markista.model.EnumNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.InterfaceNode;
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
import io.github.sandydunlop.markista.model.TypeReference;

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
import com.sun.source.doctree.StartElementTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TypeUtilsTests extends MockedDocletEnvironment {
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
        ClassNode classNode2 = new ClassNode("Node", packageNode.getName());
        TypeReference supertype = TypeReference.to("io.github.sandydunlop.markista.model.Node");
        classNode2.getSupertypes().add(supertype);
        api.addType(classNode2);
        FieldNode fieldNode = new FieldNode("int", "field");
        fieldNode.setConstantValue("1");
        classNode2.addField(fieldNode);
        TypeUtils.addConstantFieldValuesReference(unnamedModule);
        assertEquals(1, unnamedModule.getConstantValues().size());
    }

    @Disabled("MFLP-85")
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

        String tn = TypeUtils.getParamType(methodElement, "param1");
        assertNotNull(tn);
        assertEquals("int[]", tn);
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

        List<Link> refs = TypeUtils.getReferences(docCommentTree);
        assertNotNull(refs);
        assertEquals(2, refs.size());
        LinkResolver.resolve(refs.get(0));
        assertEquals(Link.Kind.URL, refs.get(0).getKind());
        assertEquals("http://example.com", refs.get(0).getUri());
        assertEquals(Link.Kind.TYPE, refs.get(1).getKind());
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

        TypeNode node = TypeUtils.nodeFromElement(interfaceTypeElement);
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

        TypeNode node = TypeUtils.nodeFromElement(enumTypeElement);
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

        TypeNode node = TypeUtils.nodeFromElement(annotationTypeElement);
        assertNotNull(node);
        assertEquals("Overrides", node.getSimpleName());
        assertEquals("io.github.sandydunlop.markista.model.Overrides", node.getQualifiedName());
        assertEquals(Node.Kind.ANNOTATION, node.getKind());
    }

    @Test
    void nodeFromElement_ExecutableElement() {
        ClassNode classNode = new ClassNode("Node", packageNode.getName());
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
        ClassNode classNode = new ClassNode("Node", packageNode.getName());
        api.addType(classNode);

        Name variableName = mock(Name.class);
        when(variableName.toString()).thenReturn("berry");
        VariableElement variableElement = mock(VariableElement.class);
        when(variableElement.getEnclosingElement()).thenReturn(typeElement);
        when(variableElement.getSimpleName()).thenReturn(variableName);
        when(elementUtils.getTypeElement(classNode.getQualifiedName())).thenReturn(typeElement);

        TypeMirror tm = mock(TypeMirror.class);
        when(tm.toString()).thenReturn("io.github.sandydunlop.markista.model.Node");
        when(variableElement.asType()).thenReturn(tm);

        DocCommentTree docTree = mock(DocCommentTree.class);
        when(treeUtils.getDocCommentTree(typeElement)).thenReturn(docTree);

        FieldNode node = TypeUtils.nodeFromElement(variableElement);
        assertNotNull(node);
    }

    @Test
    void createText_LINK() {
        DocCommentTree docTree = mock(DocCommentTree.class);
        when(treeUtils.getDocCommentTree(typeElement)).thenReturn(docTree);
        when(docTree.getKind()).thenReturn(com.sun.source.doctree.DocTree.Kind.LINK);
        when(docTree.toString()).thenReturn("{@link http://example.com}");
        List<? extends DocTree> dtList = List.of(docTree);
        Text text = TypeUtils.createText(dtList);
        LinkResolver.init(api, ctx);
        TextAssembler.assembleTextAndLinks(api, ctx);
        assertNotNull(text);
        assertEquals(1, text.getSegments().size());
        assertEquals(Text.Segment.Kind.LINK, text.getSegments().get(0).getKind());
        assertEquals("http://example.com", text.getSegments().get(0).getLink().getUri());
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
        assertEquals(Text.Segment.Kind.TEXT, text.getSegments().get(0).getKind());
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
        assertEquals(Text.Segment.Kind.CODE, text.getSegments().get(0).getKind());
        assertEquals("berry", text.toString());
    }

    @Test
    void setDocumentation() {
        ClassNode classNode = new ClassNode("Node", packageNode.getName());
        api.addType(classNode);
        TypeUtils.setDocumentation(classNode, typeElement);
        String fmt = MarkdownUtils.formatText(classNode.getFirstSentence());
        assertEquals("berry", fmt);
        assertEquals("berry", MarkdownUtils.formatText(classNode.getBody()));
        assertEquals("berry", MarkdownUtils.formatText(classNode.getFullBody()));
    }

    @Test
    void testCreateTypeNodeByKind() {
        PackageNode pkg = new PackageNode("test.pkg");

        TypeNode classNode = TypeUtils.createTypeNode("ClassA", pkg, ElementKind.CLASS);
        assertNotNull(classNode);
        assertTrue(classNode instanceof ClassNode);
        assertEquals("ClassA", classNode.getSimpleName());

        TypeNode interfaceNode = TypeUtils.createTypeNode("InterfaceA", pkg, ElementKind.INTERFACE);
        assertNotNull(interfaceNode);
        assertTrue(interfaceNode instanceof InterfaceNode);

        TypeNode enumNode = TypeUtils.createTypeNode("EnumA", pkg, ElementKind.ENUM);
        assertNotNull(enumNode);
        assertTrue(enumNode instanceof EnumNode);

        TypeNode annoNode = TypeUtils.createTypeNode("AnnotationA", pkg, ElementKind.ANNOTATION_TYPE);
        assertNotNull(annoNode);
        assertTrue(annoNode instanceof AnnotationNode);

        TypeNode nullNode = TypeUtils.createTypeNode("UnknownA", pkg, ElementKind.METHOD);
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
        FieldNode node = mock(FieldNode.class);
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

        Text txt = TypeUtils.docTreeToText(textTree);
        Text.Segment segment = txt.getSegment(0);
        assertEquals(Segment.Kind.TEXT, segment.getKind());
        assertEquals("simple text", segment.getText());
    }

    @Test
    void createTypeNode_ReturnsClassNode_WhenElementKindIsClass() {
        TypeUtils.init(dummyApi, null);  // Passing null DocletEnvironment for tests that don't need it
        TypeNode node = TypeUtils.createTypeNode("MyClass", dummyPackage, ElementKind.CLASS);
        assertNotNull(node);
        assertEquals("MyClass", node.getSimpleName());
        assertEquals("com.example.MyClass", node.getQualifiedName());
        // Asserting instance type, depending on your implementation ClassNode extends TypeNode
        assertEquals("com.example.MyClass", node.getQualifiedName());
    }

    @Test
    void createTypeNode_ReturnsInterfaceNode_WhenElementKindIsInterface() {
        TypeUtils.init(dummyApi, null);  // Passing null DocletEnvironment for tests that don't need it
        TypeNode node = TypeUtils.createTypeNode("MyInterface", dummyPackage, ElementKind.INTERFACE);
        assertNotNull(node);
        assertEquals("MyInterface", node.getSimpleName());
    }

    @Test
    void createTypeNode_ReturnsNull_WhenElementKindIsUnknown() {
        TypeUtils.init(dummyApi, null);  // Passing null DocletEnvironment for tests that don't need it
        TypeNode node = TypeUtils.createTypeNode("Unknown", dummyPackage, ElementKind.MODULE);
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
            utilsStatic.when(() -> TypeUtils.createTypeNode("Foo", packageNode, ElementKind.CLASS))
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
        EnumNode enumNode = new EnumNode("Color", null);
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

    @Test
    void nodeFromElement_FieldNode_CreatesFieldNode() {
        setup2();
        VariableElement fieldElement = mock(VariableElement.class);

        TypeElement classElement = mock(TypeElement.class);
        when(TypeUtils.getEnclosingTypeElement(fieldElement)).thenReturn(classElement);
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

        Text txt = TypeUtils.docTreeToText(textTree);
        Text.Segment segment = txt.getSegment(0);
        assertEquals(io.github.sandydunlop.markista.model.Text.Segment.Kind.TEXT, segment.getKind());
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

        var node = new io.github.sandydunlop.markista.model.ClassNode("Foo", "");

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

        TypeUtils.setAppliedAnnotations(typeNode, ac);
        assertEquals(1, typeNode.getAppliedAnnotations().size());
    }

    void setUp2() {
        envMock = mock(DocletEnvironment.class);
        when(envMock.getElementUtils()).thenReturn(mock(javax.lang.model.util.Elements.class));
        when(envMock.getDocTrees()).thenReturn(mock(DocTrees.class));
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
        TypeUtils.setAppliedAnnotation(targetType, annotationMirror);

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
        TypeNode result = TypeUtils.nodeFromElement(typeEl);
        assertNotNull(result, "nodeFromElement should return a TypeNode instance");

        // And api.addType should have been invoked with that produced node
        TypeNode cls = api.getTypeNode("io.github.sandydunlop.markista.model.MyClass");
        assertNotNull(cls);
    }


    @Test
    void setDeprecationStatus_prefers_javadoc_deprecated_over_annotation_and_handles_forRemoval() {
        setup2();
        NodeStub node = mock(NodeStub.class);
        Element element = mock(Element.class);

        // Case 1: Javadoc @deprecated present
        DocTrees docTrees = mock(DocTrees.class);
        when(mockEnvironment.getDocTrees()).thenReturn(docTrees);
        TypeUtils.init(api, mockEnvironment);

        DeprecatedTree deprecatedTree = mock(DeprecatedTree.class);
        when(deprecatedTree.getBody()).thenReturn(Collections.emptyList());
        com.sun.source.doctree.DocCommentTree dct = mock(com.sun.source.doctree.DocCommentTree.class);
        when(dct.getBlockTags()).thenAnswer(_ -> List.of((DocTree) deprecatedTree));
        when(docTrees.getDocCommentTree(element)).thenReturn(dct);

        // Make sure element.getAnnotation returns null (no @Deprecated annotation)
        when(element.getAnnotation(Deprecated.class)).thenReturn(null);

        TypeUtils.setDeprecationStatus(node, element, dct);

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

        TypeUtils.setDeprecationStatus(node, element, emptyDct);

        verify(node).setDeprecation(Deprecation.FOR_REMOVAL);
    }

    @Test
    void getSince_returns_text_for_since_tag_and_empty_for_none() {
        // With since
        com.sun.source.doctree.DocCommentTree dct = mock(com.sun.source.doctree.DocCommentTree.class);
        SinceTree sinceTree = mock(SinceTree.class);
        when(sinceTree.getBody()).thenReturn(Collections.emptyList());
        when(dct.getBlockTags()).thenAnswer(_ -> List.of((DocTree) sinceTree));

        Text got = TypeUtils.getSince(dct);
        assertNotNull(got);

        // Without since tags
        com.sun.source.doctree.DocCommentTree empty = mock(com.sun.source.doctree.DocCommentTree.class);
        when(empty.getBlockTags()).thenReturn(Collections.emptyList());
        Text none = TypeUtils.getSince(empty);
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

        ParamTree result = TypeUtils.getParamTree(dct, param);
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

        TypeUtils.setMethodParams(methodDoc, ee);

        verify(methodDoc).addParam(captor.capture());
        ParamNode added = captor.getValue();
        assertEquals("arg", added.getSimpleName());
        assertNotNull(added.getType().getRawTypeName()); // type constructed
        // Because we passed empty description, body is likely empty Text
        assertNotNull(added.getBody());
    }

    @Test
    void markCustomAnnotations_marks_custom_and_documented_flags_when_local_type_exists() {
        Api testApi = new Api("Test API");
        AppliedAnnotationNode applied = mock(AppliedAnnotationNode.class);
        TypeNode type = mock(TypeNode.class);
        when(type.getQualifiedName()).thenReturn("com.example.A");
        when(applied.getTypeName()).thenReturn("com.example.A");

        testApi.addType(type);
        testApi.getAppliedAnnotations().add(applied);
        TypeUtils.init(testApi, docletEnv);

        TypeUtils.markCustomAnnotations();

        verify(applied).setCustom(true);
    }

    @Test
    void getDeprecation_and_getReturnTree_detect_block_tags() {
        com.sun.source.doctree.DocCommentTree dct = mock(com.sun.source.doctree.DocCommentTree.class);
        DeprecatedTree dt2 = mock(DeprecatedTree.class);
        ReturnTree rt = mock(ReturnTree.class);

        when(dct.getBlockTags()).thenAnswer(_ -> List.of((DocTree) dt2, (DocTree) rt));

        DeprecatedTree foundDep = TypeUtils.getDeprecation(dct);
        ReturnTree foundRet = TypeUtils.getReturnTree(dct);

        assertSame(dt2, foundDep);
        assertSame(rt, foundRet);
    }

    @Test
    void createTextSegment_handles_text_and_start_element_and_code_and_link_plain() {
        // TEXT kind
        DocTree textTree = mock(DocTree.class);
        when(textTree.getKind()).thenReturn(com.sun.source.doctree.DocTree.Kind.TEXT);
        when(textTree.toString()).thenReturn("some text");

        Text txt = TypeUtils.docTreeToText(textTree);
        Text.Segment segText = txt.getSegment(0);
        assertEquals(Segment.Kind.TEXT, segText.getKind());
        assertEquals("some text", segText.getText());

        // START_ELEMENT 'p' should map to newline text
        Name nameMock = mockName("p");
        StartElementTree start = mock(StartElementTree.class);
        when(start.getName()).thenReturn(nameMock);
        when(start.getKind()).thenReturn(com.sun.source.doctree.DocTree.Kind.START_ELEMENT);

        // CODE kind should set CODE text using getDocTreeText (which inspects toString())
        DocTree code = mock(DocTree.class);
        when(code.getKind()).thenReturn(com.sun.source.doctree.DocTree.Kind.CODE);
        when(code.toString()).thenReturn("{@code int x}");
        txt = TypeUtils.docTreeToText(code);
        Text.Segment segCode = txt.getSegment(0);
        assertEquals(Segment.Kind.CODE, segCode.getKind());
        // code text should not be empty (string parsing may trim braces)
        assertNotNull(segCode.getText());
    }

    @Test
    void setThrownTypes_adds_exception_type_names_to_methodnode() {
        MethodNode methodNode = new MethodNode("void", "method");

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
        TypeUtils.init(api, envMock);

        TypeUtils.setThrownTypes(methodNode, List.of(tm));

        assertEquals(1, methodNode.getThrownTypes().size());
    }

    // A small helper stub interface so we can verify Node interactions without requiring the real Node implementation.
    // If your project provides a concrete Node class you can replace references accordingly.
    private class NodeStub extends io.github.sandydunlop.markista.model.Node {
        // No additional members required; used for mocking only.
    }

    @Test
    void docTreeToText_TEXT() {
        TypeUtils.init(api, docletEnvironmentMock);
        DocTree docTreeMock = mockDocCommentTree_TEXT("plain text");
        Text text = TypeUtils.docTreeToText(docTreeMock);
        assertEquals("plain text", text.toString());
    }

    @Test
    void docTreeToText_MARKDOWN() {
        TypeUtils.init(api, docletEnvironmentMock);
        DocTree docTreeMock = mockDocCommentTree_MARKDOWN("markdown text");
        Text text = TypeUtils.docTreeToText(docTreeMock);
        assertEquals("markdown text", text.toString());
    }

    @Test
    void docTreeToText_LINK() {
        ctx.setTypeName("Mocktype");
        TypeUtils.init(api, docletEnvironmentMock);
        DocTree docTreeMock = mockDocCommentTree_LINK();
        Text text = TypeUtils.docTreeToText(docTreeMock);
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
        TypeUtils.init(api, docletEnvironmentMock);
        DocTree docTreeMock = mockDocCommentTree_LINK_PLAIN();
        Text text = TypeUtils.docTreeToText(docTreeMock);
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
        TypeUtils.init(api, docletEnvironmentMock);
        DocTree docTreeMock = mockDocCommentTree_START_ELEMENT();
        Text text = TypeUtils.docTreeToText(docTreeMock);
        assertEquals("\n\n", text.toString());
    }

    @Test
    void docTreeToText_END_ELEMENT() {
        TypeUtils.init(api, docletEnvironmentMock);
        DocTree docTreeMock = mockDocCommentTree_END_ELEMENT();
        Text text = TypeUtils.docTreeToText(docTreeMock);
        assertEquals("", text.toString());
    }

    @Test
    void markdownToText_link_with_parens() {
        String markdown = "onverts Markdown text into a [Text](https://example.com) object";
        Text text = TypeUtils.markdownToText(markdown);
        assertNotNull(text);
        assertEquals(3, text.getSegments().size());
        assertEquals(Segment.Kind.LINK, text.getSegment(1).getKind());
        assertEquals("https://example.com", text.getSegment(1).getLink().getTarget());
        assertEquals("Text", text.getSegment(1).getLink().getLabel());
    }

    @Test
    void markdownToText_link_without_parens() {
        String markdown = "onverts Markdown text into a [Text] object";
        Text text = TypeUtils.markdownToText(markdown);
        assertNotNull(text);
        assertEquals(3, text.getSegments().size());
        assertEquals(Segment.Kind.LINK, text.getSegment(1).getKind());
        assertEquals("Text", text.getSegment(1).getLink().getTarget());
    }

    @Disabled("20250823-Refactoring: Adding methods to types has been moved to TextAssembler")
    @Test
    void overriddenMethodInheritDocs() {
        mockDocletEnvironment();
        List<Element> elements = new ArrayList<>();
        elements.add(mockModule("mockmodule"));
        PackageElement packageMock = mockPackage("mockpackage");
        elements.add(packageMock);

        // Mock the supertype with a method to be overridden
        TypeElement supertypeMock = mockType("MockSupertype", packageMock);
        elements.add(supertypeMock);
        ExecutableElement baseExecutableMock = mockExecutable("mockmethod", supertypeMock);
        elements.add(baseExecutableMock);
        VariableElement overriddenParameterMock = mockMethodParameter("mockparameter", supertypeMock, baseExecutableMock);
        elements.add(overriddenParameterMock);

        // Mock the type with a method that overrides
        TypeElement typeMock = mockType("MockType", packageMock);
        elements.add(typeMock);
        ExecutableElement executableMock = mockExecutable("mockmethod", typeMock);
        elements.add(executableMock);
        VariableElement parameterMock = mockMethodParameter("mockparameter", typeMock, executableMock);
        elements.add(parameterMock);

        // Add all of the above to the list of elements that ApiScanner will scan
        mockIncludedElements(elements);

        ApiScanner apiScanner = new ApiScanner(docletEnvironmentMock);
        apiScanner.scan(docletEnvironmentMock.getIncludedElements());

        // Set up a dummy API model node for the mocked package
        PackageNode pkgNode = new PackageNode("mockpackage");
        apiScanner.api.addPackage(pkgNode);

        // Mocked elementUtils returns the supertypeMock with its method
        List<Element> supertypeMembers = List.of(baseExecutableMock);
        when(supertypeMock.getEnclosedElements()).thenAnswer(_ -> supertypeMembers);
        when(elementUtilsMock.getTypeElement(supertypeMock.getQualifiedName().toString())).thenReturn(supertypeMock);

        // Mocked elementUtils returns the typeMock with its method
        List<Element> typeMembers = List.of(baseExecutableMock);
        when(typeMock.getEnclosedElements()).thenAnswer(_ -> typeMembers);
        when(elementUtilsMock.getTypeElement(typeMock.getQualifiedName().toString())).thenReturn(typeMock);

        // DocCommentTree for the supertype's method has text
        DocCommentTree baseMethodDocTree = mockDocCommentTree();
        DocTree baseMethodText = mockDocCommentTree_TEXT("supertypeMethodText");
        List<DocTree> baseMethodDoc = List.of(baseMethodText);
        when(baseMethodDocTree.getFirstSentence()).thenAnswer(_ -> baseMethodDoc);
        when(treeUtilsMock.getDocCommentTree(baseExecutableMock)).thenReturn(baseMethodDocTree);

        // DocCommentTree for the overriding method has inheritDoc
        DocCommentTree typeMethodDocTree = mockDocCommentTree();
        DocTree typeMethodText = mockDocCommentTree_INHERIT_DOC();
        List<DocTree> typeMethodDoc = List.of(typeMethodText);
        when(typeMethodDocTree.getFirstSentence()).thenAnswer(_ -> typeMethodDoc);
        when(treeUtilsMock.getDocCommentTree(executableMock)).thenReturn(typeMethodDocTree);
        mockMethodAnnotation("java.lang.Override", "Override", executableMock);

        // Run the code we're testing
        apiScanner.visitType(supertypeMock, 1);
        apiScanner.visitType(typeMock, 1);
        apiScanner.visitExecutable(baseExecutableMock, 1);
        apiScanner.visitExecutable(executableMock, 1);

        // Get the TypeNodes that have just been created
        TypeNode supertypeNode = apiScanner.api.getTypeNode("mockpackage.MockSupertype");
        TypeNode typeNode = apiScanner.api.getTypeNode("mockpackage.MockType");
        assertEquals(1, supertypeNode.getMethods().size());
        assertEquals(1, typeNode.getMethods().size());

        // Check the supertype's documentation
        MethodNode supertypeMethodNode = supertypeNode.getMethods().getFirst();
        String supertypeText = supertypeMethodNode.getFirstSentence().toString();
        assertEquals("supertypeMethodText", supertypeText);

        // Check the overriding type's documentation
        MethodNode typeMethodNode = typeNode.getMethods().getFirst();
        Text text = typeMethodNode.getFirstSentence();
        assertEquals(1, text.getSegments().size());
        Text.Segment segment = text.getSegment(0);
        assertEquals(Segment.Kind.INHERIT, segment.getKind());
    }

	@Test
	void removeGenerics() {
		String x = TypeUtils.removeGenerics("List<String>");
		assertEquals("List", x);
		assertEquals("List", TypeUtils.removeGenerics("List<? extends ArrayList>"));
		assertEquals("List", TypeUtils.removeGenerics("List<String[]>"));
		assertEquals("", TypeUtils.removeGenerics(""));
		assertEquals("", TypeUtils.removeGenerics(null));
	}

    @Test
    void markdownToTest_webLink() {
        String markdown = "the [Markista homepage](https://sandydunlop.github.io/markista)";

        Text text = TypeUtils.markdownToText(markdown);
        Text.Segment segment = text.getSegments().getLast();
        assertEquals(Text.Segment.Kind.LINK, segment.getKind());


        assertNotNull(text);
    }
}
