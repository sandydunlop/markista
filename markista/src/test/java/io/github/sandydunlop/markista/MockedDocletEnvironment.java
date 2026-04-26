package io.github.sandydunlop.markista;

import io.github.sandydunlop.markista.core.Configuration;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.doclet.MarkdownDoclet;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTree.Kind;
import com.sun.source.doctree.InheritDocTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockedDocletEnvironment {
    protected Context ctx = Context.getInstance();
    protected MarkdownDoclet doclet;

    protected DocletEnvironment docletEnvironmentMock;
    protected Elements elementUtilsMock;
    protected Types typeUtilsMock;
    protected DocTrees treeUtilsMock;

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

    protected void mockDocletEnvironment() {
        resetConfiguration();
        Configuration.setVerbose(true);
        docletEnvironmentMock = mock(DocletEnvironment.class);
        elementUtilsMock = mock(Elements.class);
        typeUtilsMock = mock(Types.class);
        treeUtilsMock = mock(DocTrees.class);
        when(docletEnvironmentMock.getElementUtils()).thenReturn(elementUtilsMock);
        when(docletEnvironmentMock.getTypeUtils()).thenReturn(typeUtilsMock);
        when(docletEnvironmentMock.getDocTrees()).thenReturn(treeUtilsMock);
        doclet = new MarkdownDoclet();
        doclet.init(Locale.US, reporter);
    }

    protected void resetConfiguration() {
        Configuration.setDocTitle("API");
        Configuration.setDocumentPrivateMembers(false);
        Configuration.setExtensionsOrder(null);
        Configuration.setFlattenModules(false);
        Configuration.setFlattenPackages(false);
        Configuration.setAddModules("");
        Configuration.setModulePaths("");
        Configuration.setProjectPath(null);
        Configuration.setUseContentTabs(false);
        Configuration.setVerbose(false);
    }

    protected Name mockName(String n) {
        Name nameMock = mock(Name.class);
        when(nameMock.toString()).thenReturn(n);
        return nameMock;
    }

    protected TypeMirror mockMirror(String name) {
        TypeMirror mockTypeMirror = mock(TypeMirror.class);
        when(mockTypeMirror.toString()).thenReturn(name);
        return mockTypeMirror;
    }

    protected DocCommentTree mockDocCommentTree() {
        DocCommentTree dct = mock(DocCommentTree.class);
        when(dct.getKind()).thenReturn(Kind.DOC_COMMENT);
        return dct;
    }

    protected DocCommentTree mockDocCommentTree_MARKDOWN(String string) {
        DocCommentTree dct = mock(DocCommentTree.class);
        when(dct.getKind()).thenReturn(Kind.MARKDOWN);
        when(dct.toString()).thenReturn(string);
        return dct;
    }

    protected DocCommentTree mockDocCommentTree_TEXT(String string) {
        DocCommentTree dct = mock(DocCommentTree.class);
        when(dct.getKind()).thenReturn(Kind.TEXT);
        when(dct.toString()).thenReturn(string);
        return dct;
    }

    protected LinkTree mockDocCommentTree_LINK() {
        ReferenceTree rt = mock(ReferenceTree.class);
        when (rt.getSignature()).thenReturn("io.github.qishr.cascara.lang.java.model.Node");
        LinkTree dct = mock(LinkTree.class);
        when (dct.getReference()).thenReturn(rt);
        when(dct.getKind()).thenReturn(Kind.LINK);
        when(dct.toString()).thenReturn("{@link io.github.qishr.cascara.lang.java.model.Node}");
        return dct;
    }

    protected DocTree mockDocCommentTree_LINK_PLAIN() {
        ReferenceTree rt = mock(ReferenceTree.class);
        when (rt.getSignature()).thenReturn("io.github.qishr.cascara.lang.java.model.Node");
        DocTree linkText = mockDocCommentTree_MARKDOWN("link text");
        LinkTree dct = mock(LinkTree.class);
        when (dct.getReference()).thenReturn(rt);
        List<DocTree> list = List.of(linkText);
        when(dct.getKind()).thenReturn(Kind.LINK_PLAIN);
        when(dct.toString()).thenReturn("{@link io.github.qishr.cascara.lang.java.model.Node link text}");
        when(dct.getLabel()).thenAnswer(_ -> list);
        return dct;
    }

    protected DocTree mockDocCommentTree_START_ELEMENT() {
        Name name = mockName("p");
        StartElementTree dct = mock(StartElementTree.class);
        when(dct.getKind()).thenReturn(Kind.START_ELEMENT);
        when(dct.getName()).thenReturn(name);
        return dct;
    }

    protected DocTree mockDocCommentTree_END_ELEMENT() {
        DocTree dct = mock(DocTree.class);
        when(dct.getKind()).thenReturn(Kind.END_ELEMENT);
        return dct;
    }

    protected DocTree mockDocCommentTree_INHERIT_DOC() {
        InheritDocTree dct = mock(InheritDocTree.class);
        when(dct.getKind()).thenReturn(Kind.INHERIT_DOC);
        return dct;
    }

    protected URI mockUri(String path) {
        URI uriMock = mock(URI.class);
        when(uriMock.isAbsolute()).thenReturn(true);
        when(uriMock.isOpaque()).thenReturn(false);
        when(uriMock.getScheme()).thenReturn("file");
        when(uriMock.getPath()).thenReturn(path);
        return uriMock;
    }

    protected ModuleElement mockModule(String name) {
        ModuleElement mockModuleElement = mock(ModuleElement.class);
        Name moduleName = mockName(name);
        when (mockModuleElement.getQualifiedName()).thenReturn(moduleName);
        when (mockModuleElement.getKind()).thenReturn(ElementKind.MODULE);
        URI mockModuleFileUri = mockUri("/module-info.java");
        JavaFileObject mockJFO = mock(JavaFileObject.class);
        when(mockJFO.getName()).thenReturn("file:///module-info.java");
        when(mockJFO.toUri()).thenReturn(mockModuleFileUri);
        when (elementUtilsMock.getFileObjectOf(mockModuleElement)).thenReturn(mockJFO);
        return mockModuleElement;
    }

    protected PackageElement mockPackage(String name) {
        PackageElement mockPackageElement = mock(PackageElement.class);
        Name packageName = mockName(name);
        when (mockPackageElement.getQualifiedName()).thenReturn(packageName);
        when (mockPackageElement.getKind()).thenReturn(ElementKind.PACKAGE);
        return mockPackageElement;
    }

    protected TypeElement mockType(String name, PackageElement mockPackageElement) {
        TypeElement mockTypeElement = mock(TypeElement.class);
        Name typeName = mockName(name);
        Name qualifiedName = mockName(mockPackageElement.getQualifiedName().toString() + "." + name);
        when (mockTypeElement.getQualifiedName()).thenReturn(qualifiedName);
        when (mockTypeElement.getSimpleName()).thenReturn(typeName);
        Set<Modifier> modifierSet = new HashSet<>(List.of(Modifier.PUBLIC));
        when (mockTypeElement.getModifiers()).thenReturn(modifierSet);
        when (mockTypeElement.getEnclosingElement()).thenReturn(mockPackageElement);
        when (mockTypeElement.getKind()).thenReturn(ElementKind.CLASS);
        when (elementUtilsMock.getPackageOf(mockTypeElement)).thenReturn(mockPackageElement);

        TypeMirror tm = mock(TypeMirror.class);
        when(tm.toString()).thenReturn(name);
        when(mockTypeElement.asType()).thenReturn(tm);

        return mockTypeElement;
    }

    protected VariableElement mockVariable(String name, TypeElement mockTypeElement) {
        VariableElement mockVariableElement = mock(VariableElement.class);
        Name typeName = mockName(name);
        Set<Modifier> modifierSet = new HashSet<>(List.of(Modifier.PUBLIC));
        TypeMirror mockTypeMirror = mockMirror("");
        when (mockVariableElement.getSimpleName()).thenReturn(typeName);
        when (mockVariableElement.getModifiers()).thenReturn(modifierSet);
        when (mockVariableElement.getKind()).thenReturn(ElementKind.FIELD);
        when (mockVariableElement.getEnclosingElement()).thenReturn(mockTypeElement);
        when (mockVariableElement.asType()).thenReturn(mockTypeMirror);
        return mockVariableElement;
    }

    protected ExecutableElement mockExecutable(String name, TypeElement mockTypeElement) {
        ExecutableElement mockExecutableElement = mock(ExecutableElement.class);
        Name typeName = mockName(name);
        Set<Modifier> modifierSet = new HashSet<>(List.of(Modifier.PUBLIC));
        TypeMirror mockTypeMirror = mockMirror("");
        when (mockExecutableElement.getSimpleName()).thenReturn(typeName);
        when (mockExecutableElement.getModifiers()).thenReturn(modifierSet);
        when (mockExecutableElement.getKind()).thenReturn(ElementKind.METHOD);
        when (mockExecutableElement.getEnclosingElement()).thenReturn(mockTypeElement);
        when (mockExecutableElement.getReturnType()).thenReturn(mockTypeMirror);
        return mockExecutableElement;
    }

    protected ExecutableElement mockConstructor(TypeElement mockTypeElement) {
        ExecutableElement mockExecutableElement = mock(ExecutableElement.class);
        Name typeName = mockName("<init>");
        Set<Modifier> modifierSet = new HashSet<>(List.of(Modifier.PUBLIC));
        TypeMirror mockTypeMirror = mockMirror("");
        when (mockExecutableElement.getSimpleName()).thenReturn(typeName);
        when (mockExecutableElement.getModifiers()).thenReturn(modifierSet);
        when (mockExecutableElement.getKind()).thenReturn(ElementKind.CONSTRUCTOR);
        when (mockExecutableElement.getEnclosingElement()).thenReturn(mockTypeElement);
        when (mockExecutableElement.getReturnType()).thenReturn(mockTypeMirror);
        return mockExecutableElement;
    }

    protected TypeParameterElement mockTypeParameter(String name, TypeElement mockTypeElement) {
        TypeParameterElement mockParameterElement = mock(TypeParameterElement.class);
        Name typeName = mockName(name);
        Set<Modifier> modifierSet = new HashSet<>(List.of(Modifier.PUBLIC));
        TypeMirror mockTypeMirror = mockMirror("");
        when (mockParameterElement.getSimpleName()).thenReturn(typeName);
        when (mockParameterElement.getModifiers()).thenReturn(modifierSet);
        when (mockParameterElement.getKind()).thenReturn(ElementKind.FIELD);
        when (mockParameterElement.getEnclosingElement()).thenReturn(mockTypeElement);
        when (mockParameterElement.asType()).thenReturn(mockTypeMirror);
        return mockParameterElement;
    }

    protected VariableElement mockMethodParameter(String name, TypeElement mockTypeElement, ExecutableElement mockMethod) {
        VariableElement mockParameterElement = mock(VariableElement.class);
        Name typeName = mockName(name);
        Set<Modifier> modifierSet = new HashSet<>(List.of(Modifier.PUBLIC));
        TypeMirror mockTypeMirror = mockMirror("");
        when (mockParameterElement.getSimpleName()).thenReturn(typeName);
        when (mockParameterElement.getModifiers()).thenReturn(modifierSet);
        when (mockParameterElement.getKind()).thenReturn(ElementKind.PARAMETER);
        when (mockParameterElement.getEnclosingElement()).thenReturn(mockTypeElement);
        when (mockParameterElement.asType()).thenReturn(mockTypeMirror);

        List<VariableElement> paramList = new ArrayList<>();
        paramList.add(mockParameterElement);
        when (mockMethod.getParameters()).thenAnswer(_ -> paramList);

        return mockParameterElement;
    }

    protected AnnotationMirror mockMethodAnnotation(String qualifiedName, String simpleName, ExecutableElement mockMethod) {
        Name qName = mockName(qualifiedName);
        Name sName = mockName(simpleName);
        TypeElement element = mock(TypeElement.class);
        when (element.getQualifiedName()).thenReturn(qName);
        when (element.getSimpleName()).thenReturn(sName);

        DeclaredType dt = mock(DeclaredType.class);
        when (dt.asElement()).thenReturn(element);

        AnnotationMirror mirror = mock(AnnotationMirror.class);
        when (mirror.getAnnotationType()).thenReturn(dt);

        List<AnnotationMirror> annoList = List.of(mirror);
        when (mockMethod.getAnnotationMirrors()).thenAnswer(_ -> annoList);
        return mirror;
    }

    protected void mockIncludedElements(List<Element> elementsList) {
        Set<? extends Element> elementsSet = new HashSet<>(elementsList);
        when(docletEnvironmentMock.getIncludedElements()).thenAnswer(_ -> elementsSet);
    }

    protected JavaFileObject mockJavaFileObject(TypeElement typeElement) {
        String path = "/tmp/" + typeElement.getSimpleName().toString() + ".java";
        URI mockClassFileUri = mockUri(path);
        JavaFileObject mockJFO = mock(JavaFileObject.class);
        when(mockJFO.getName()).thenReturn("file://" + path);
        when(mockJFO.toUri()).thenReturn(mockClassFileUri);
        when (elementUtilsMock.getFileObjectOf(any())).thenAnswer(_ -> {
            return mockJFO;
        });
        return mockJFO;
    }

    protected void mockDeprecation(ExecutableElement methodElement, boolean forRemoval) {
        DeprecatedTree deprecatedTree = mock(DeprecatedTree.class);
        when(deprecatedTree.getBody()).thenReturn(Collections.emptyList());
        DocCommentTree dct = mock(DocCommentTree.class);
        when(dct.getBlockTags()).thenAnswer(_ -> List.of((DocTree) deprecatedTree));
        when(treeUtilsMock.getDocCommentTree(methodElement)).thenReturn(dct);

        Deprecated deprecatedAnno = mock(Deprecated.class);
        when(deprecatedAnno.forRemoval()).thenReturn(forRemoval);
        when(methodElement.getAnnotation(Deprecated.class)).thenReturn(deprecatedAnno);
    }
}
