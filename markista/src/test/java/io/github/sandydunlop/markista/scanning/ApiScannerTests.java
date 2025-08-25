package io.github.sandydunlop.markista.scanning;

import io.github.sandydunlop.markista.MockedDocletEnvironment;
import io.github.sandydunlop.markista.core.Configuration;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.Text;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.model.TypeView;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.DirectiveKind;
import javax.lang.model.element.ModuleElement.ExportsDirective;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiScannerTests extends MockedDocletEnvironment {
    private static Context ctx;

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
        ctx = Context.getInstance();
		ctx.setReporter(reporter);
        ctx.setOutputDirectory("/tmp/doc");
    }

    @BeforeEach
    void init() {
        // Nothing to see here
    }

    @Test
    void visitModule_named() {
        final String MODULE_NAME = "module.name";
        final String MODULE_NAME2 = "module.two";
        final String PACKAGE_NAME = "package.name";

        Name moduleName = mock(Name.class);
        Mockito.when(moduleName.toString()).thenReturn(MODULE_NAME);
        ModuleElement moduleElement = mock(ModuleElement.class);
        Mockito.when(moduleElement.getQualifiedName()).thenReturn(moduleName);

        Name moduleName2 = mock(Name.class);
        Mockito.when(moduleName2.toString()).thenReturn(MODULE_NAME2);
        ModuleElement moduleElement2 = mock(ModuleElement.class);
        Mockito.when(moduleElement2.getQualifiedName()).thenReturn(moduleName2);

        DocTrees docTrees2 = mock(DocTrees.class);
        DocletEnvironment mockEnvironment = mock(DocletEnvironment.class);
        Mockito.when(mockEnvironment.getDocTrees()).thenReturn(docTrees2);
        TypeUtils.init(null, mockEnvironment);

        JavaFileObject jfo = mock(JavaFileObject.class);
        Mockito.when(jfo.getName()).thenReturn("module-info.java");
        Mockito.when(jfo.toUri()).thenReturn(Path.of("").toUri());
        Elements elementUtils2 = mock(Elements.class);
        Mockito.when(elementUtils.getFileObjectOf(any())).thenReturn(jfo);
        Mockito.when(mockEnvironment.getElementUtils()).thenReturn(elementUtils2);

        Name packageName = mock(Name.class);
        Mockito.when(packageName.toString()).thenReturn(PACKAGE_NAME);
        PackageElement packageElement = mock(PackageElement.class);
        Mockito.when(packageElement.getQualifiedName()).thenReturn(packageName);

        ExportsDirective directive = mock(ExportsDirective.class);
        Mockito.when(directive.getKind()).thenReturn(DirectiveKind.EXPORTS);
        Mockito.when(directive.getPackage()).thenReturn(packageElement);
        Mockito.when(directive.getTargetModules()).thenAnswer(_ -> List.of(moduleElement2));

        Mockito.when(moduleElement.getDirectives()).thenAnswer(_ -> List.of(directive));

        ApiScanner scanner2 = new ApiScanner(mockEnvironment);
        scanner2.visitModule(moduleElement, Integer.valueOf(0));

        Api api = scanner2.api;
        assertEquals(1, api.getModules().size());
        ModuleNode moduleNode = api.getModules().get(0);
        assertEquals(MODULE_NAME, moduleNode.getName());
    }
    
    @Test
    void visitModule_unnamed() {
        Name moduleName = mock(Name.class);
        Mockito.when(moduleName.toString()).thenReturn("");
        ModuleElement moduleElement = mock(ModuleElement.class);
        Mockito.when(moduleElement.getQualifiedName()).thenReturn(moduleName);

        DocTrees docTrees2 = mock(DocTrees.class);
        DocletEnvironment mockEnvironment = mock(DocletEnvironment.class);
        Mockito.when(mockEnvironment.getDocTrees()).thenReturn(docTrees2);
        TypeUtils.init(null, mockEnvironment);

        ApiScanner scanner2 = new ApiScanner(mockEnvironment);
        scanner2.visitModule(moduleElement, Integer.valueOf(0));

        Api api = scanner2.api;
        assertEquals(0, api.getModules().size());
    }




    private DocletEnvironment env;
    private DocTrees docTrees;
    private javax.lang.model.util.Elements elementUtils;
    private ApiScanner scanner;

    @BeforeEach
    void setUp() {
        env = mock(DocletEnvironment.class);
        docTrees = mock(DocTrees.class);
        elementUtils = mock(javax.lang.model.util.Elements.class);

        when(env.getDocTrees()).thenReturn(docTrees);
        when(env.getElementUtils()).thenReturn(elementUtils);

        // Construct scanner with mocked doclet environment
        scanner = new ApiScanner(env);
    }

    private void invokeProcessIncludedElements(Set<? extends Element> elements) throws Exception {
        Method proc = ApiScanner.class.getDeclaredMethod("processIncludedElements", Set.class);
        proc.setAccessible(true);
        proc.invoke(scanner, elements);
    }

    @Disabled("WIP")
    @Test
    void visitType_setsSourcePath_and_packageSourcePath_when_jfo_present() throws Exception {
        TypeElement typeEl = mock(TypeElement.class);
        Name qname = mock(Name.class);
        when(qname.toString()).thenReturn("com.example.Type");
        when(typeEl.getQualifiedName()).thenReturn(qname);

        // Ensure the scanner considers this element included
        invokeProcessIncludedElements(Set.of(typeEl));

        // Mock TypeUtils static behavior
        TypeNode typeNode = mock(TypeNode.class);
        PackageNode pkg = mock(PackageNode.class);
        when(typeNode.getPackageName()).thenReturn(pkg.getName());
        when(pkg.getSourcePath()).thenReturn(null);

        try (MockedStatic<TypeUtils> t = Mockito.mockStatic(TypeUtils.class)) {
            t.when(() -> TypeUtils.isIncludedInApi(typeEl)).thenReturn(true);
            t.when(() -> TypeUtils.nodeFromElement(typeEl)).thenReturn(typeNode);

            // Make elementUtils provide a JavaFileObject for the type element
            JavaFileObject jfo = mock(JavaFileObject.class);
            when(elementUtils.getFileObjectOf(typeEl)).thenReturn(jfo);
            URI uri = new File("src/com/example/Type.java").toURI();
            when(jfo.toUri()).thenReturn(uri);

            // Call visitType
            scanner.visitType(typeEl, 0);

            // Verify TypeUtils.nodeFromElement used and setDocumentation called
            t.verify(() -> TypeUtils.nodeFromElement(typeEl));

            // Verify the type node had its source path set to the JFO uri path
            verify(typeNode).setSourcePath(uri.toString());

            // Because pkg.getSourcePath returned null, package.setSourcePath should be called with parent path
            verify(pkg).setSourcePath(Path.of(uri).getParent().toString());
        }
    }

    @Disabled("WIP")
    @Test
    void visitExecutable_setsDocumentationFields_when_docComment_present() throws Exception {
        // Setup a TypeElement and an ExecutableElement whose enclosing element is the type
        TypeElement typeEl = mock(TypeElement.class);
        Name qname = mock(Name.class);
        when(qname.toString()).thenReturn("com.example.Type");
        when(typeEl.getQualifiedName()).thenReturn(qname);

        ExecutableElement exec = mock(ExecutableElement.class);
        when(exec.getEnclosingElement()).thenReturn(typeEl);

        // Ensure the scanner's includedNames contains the type's qualified name
        invokeProcessIncludedElements(Set.of(typeEl));

        MethodNode mnode = mock(MethodNode.class);
        DocCommentTree dct = mock(DocCommentTree.class);
        when(env.getDocTrees().getDocCommentTree(exec)).thenReturn(dct);

        // Provide some dummy first sentence/body to be passed through TypeUtils.createText
        when(dct.getFirstSentence()).thenReturn(Collections.emptyList());
        when(dct.getBody()).thenReturn(Collections.emptyList());
        when(dct.getFullBody()).thenReturn(Collections.emptyList());

        try (MockedStatic<TypeUtils> t = Mockito.mockStatic(TypeUtils.class)) {
            t.when(() -> TypeUtils.isIncludedInApi(exec)).thenReturn(true);
            t.when(() -> TypeUtils.nodeFromElement(exec)).thenReturn(mnode);
            t.when(() -> TypeUtils.setDeprecationStatus(mnode, exec, dct)).thenAnswer(_ -> null);
            t.when(() -> TypeUtils.createText(dct.getFirstSentence())).thenReturn(Text.of("first"));
            t.when(() -> TypeUtils.createText(dct.getBody())).thenReturn(Text.of("body"));
            t.when(() -> TypeUtils.createText(dct.getFullBody())).thenReturn(Text.of("full"));
            t.when(() -> TypeUtils.getReturnTree(dct)).thenReturn((ReturnTree) null);
            t.when(() -> TypeUtils.getReferences(dct)).thenReturn(Collections.emptyList());
            t.when(() -> TypeUtils.getSince(dct)).thenReturn(null);

            scanner.visitExecutable(exec, 0);

            // Verify TypeUtils methods used
            t.verify(() -> TypeUtils.nodeFromElement(exec));
            t.verify(() -> TypeUtils.setDeprecationStatus(mnode, exec, dct));
            t.verify(() -> TypeUtils.createText(dct.getFirstSentence()), atLeastOnce());
            t.verify(() -> TypeUtils.createText(dct.getBody()), atLeastOnce());
            t.verify(() -> TypeUtils.createText(dct.getFullBody()), atLeastOnce());
            t.verify(() -> TypeUtils.getReferences(dct));
            t.verify(() -> TypeUtils.getSince(dct));

            // Verify the method node got its textual fields set (via mock invocations)
            verify(mnode).setFirstSentence(Text.of("first"));
            verify(mnode).setBody(Text.of("body"));
            verify(mnode).setFullBody(Text.of("full"));
            verify(mnode).setReferences(Collections.emptyList());
            verify(mnode).setSince(null);
        }
    }

    @Disabled("WIP")
    @Test
    void visitVariable_setsConstant_and_documentation_and_modifiers() throws Exception {

        TypeElement typeEl = mock(TypeElement.class);
        Name qname = mock(Name.class);
        when(qname.toString()).thenReturn("com.example.Type");
        when(typeEl.getQualifiedName()).thenReturn(qname);
        when(typeEl.getKind()).thenReturn(ElementKind.CLASS);

        Name name = mock(Name.class);
        when(name.toString()).thenReturn("Type");
        VariableElement ve = mock(VariableElement.class);
        when(ve.getEnclosingElement()).thenReturn(typeEl);
        when(ve.getKind()).thenReturn(ElementKind.FIELD);
        when(ve.getSimpleName()).thenReturn(name);

        // ensure included
        invokeProcessIncludedElements(Set.of(typeEl));

        Api api2 = new Api("berry");
        ClassNode berry = new ClassNode("Type", null);
        api2.addType(berry);
        DocletEnvironment mockEnvironment = mock(DocletEnvironment.class);
        TypeUtils.init(api2, mockEnvironment);

        Configuration.setDocumentPrivateMembers(true); 
            TypeUtils.ctx = ctx;
            scanner.visitVariable(ve, 0);

        
        assertNotNull(berry.getFirstSentence());
    }

    @Test
    void visitPackage_createsPackageNode_and_setsDocumentation_and_sourcePath() throws Exception {
        PackageElement pkgElement = mock(PackageElement.class);
        Name qname = mock(Name.class);
        when(qname.toString()).thenReturn("com.example");
        when(pkgElement.getQualifiedName()).thenReturn(qname);
        // Mark the package as included
        invokeProcessIncludedElements(Set.of(pkgElement));

        try (MockedStatic<TypeUtils> t = Mockito.mockStatic(TypeUtils.class)) {
            t.when(() -> TypeUtils.setPackageSourcePath(any(PackageNode.class), eq(pkgElement))).thenAnswer(_ -> null);
            t.when(() -> TypeUtils.setDocumentation(any(), eq(pkgElement))).thenAnswer(_ -> null);

            // Call visitPackage
            scanner.visitPackage(pkgElement, 0);

            // After visiting, the Api inside scanner should contain the package node
            Api api = scanner.api;
            assertNotNull(api.getPackageNode("com.example"), "package should have been added to API model");

            // The added package should be associated with the current module (unnamed module initially)
            ModuleNode unnamed = api.getUnnamedModuleNode();
            boolean found = unnamed.getPackages().stream()
                    .anyMatch(p -> "com.example".equals(((PackageNode) p).getName()));
            assertTrue(found, "Unnamed module should contain the added package");
        }
    }

    @Test
    void calculateUnnamedModuleSourcePath_computes_root_and_sets_module_source_path() throws Exception {
        Api api = scanner.api;
        ModuleNode unnamed = api.getUnnamedModuleNode();

        // Create a mocked PackageNode and attach to unnamed module packages deque
        PackageNode pkg = mock(PackageNode.class);
        when(pkg.getName()).thenReturn("com.example");
        Path srcPath = Path.of("root", "src", "com", "example");
        when(pkg.getSourcePath()).thenReturn(srcPath.toString());

        // Add to unnamed module packages (LinkedList or similar)
        unnamed.getPackages().add(pkg);

        // Call the private calculateUnnamedModuleSourcePath method via reflection
        Method m = ApiScanner.class.getDeclaredMethod("calculateUnnamedModuleSourcePath");
        m.setAccessible(true);
        m.invoke(scanner);

        // Expected root computed by replacing nameAsPath in the source path string
        String separator = java.nio.file.FileSystems.getDefault().getSeparator();
        String nameAsPath = pkg.getName().replace(".", separator);
        String expectedRootStr = pkg.getSourcePath().replace(nameAsPath, "");

        assertEquals(expectedRootStr, unnamed.getSourcePath(), "Unnamed module sourcePath should be computed from package source path");
    }

    @Test
    void processIncludedElements_and_isIncludedElement_behavior() throws Exception {
        // Create a package and a type element
        PackageElement pkgElement = mock(PackageElement.class);
        Name pkgName = mock(Name.class);
        when(pkgName.toString()).thenReturn("com.test.pkg");
        when(pkgElement.getQualifiedName()).thenReturn(pkgName);

        TypeElement typeElement = mock(TypeElement.class);
        Name typeQName = mock(Name.class);
        when(typeQName.toString()).thenReturn("com.test.pkg.Type");
        when(typeElement.getQualifiedName()).thenReturn(typeQName);

        Set<Element> elements = new HashSet<>();
        elements.add(pkgElement);
        elements.add(typeElement);

        // Invoke private processIncludedElements
        Method proc = ApiScanner.class.getDeclaredMethod("processIncludedElements", Set.class);
        proc.setAccessible(true);
        proc.invoke(scanner, elements);

        // Access private isIncludedElement(String) and isIncludedElement(Element) via reflection
        Method isIncludedByName = ApiScanner.class.getDeclaredMethod("isIncludedElement", String.class);
        isIncludedByName.setAccessible(true);
        Method isIncludedByElement = ApiScanner.class.getDeclaredMethod("isIncludedElement", Element.class);
        isIncludedByElement.setAccessible(true);

        // Names that were added should be included
        assertTrue((Boolean) isIncludedByName.invoke(scanner, "com.test.pkg"));
        assertTrue((Boolean) isIncludedByName.invoke(scanner, "com.test.pkg.Type"));

        // For element overload: create an element whose enclosing element is a TypeElement with matching qualified name
        VariableElement someVar = mock(VariableElement.class);
        when(someVar.getEnclosingElement()).thenReturn(typeElement);

        assertTrue((Boolean) isIncludedByElement.invoke(scanner, someVar));
    }

    @Disabled("Needs fixing")
    @Test
    void scan_invokes_TypeUtils_init_and_returns_api_and_registers_included_elements() {
        // Prepare a package element to feed into scan
        PackageElement pkgElement = mock(PackageElement.class);
        Name qname = mock(Name.class);
        when(qname.toString()).thenReturn("com.scan.pkg");
        when(pkgElement.getQualifiedName()).thenReturn(qname);

        Set<Element> elements = Collections.singleton(pkgElement);

        // Mock TypeUtils static methods used inside scan to be no-ops
        try (MockedStatic<TypeUtils> t = Mockito.mockStatic(TypeUtils.class)) {
            t.when(() -> TypeUtils.init(any(), eq(env))).thenAnswer(_ -> null);
            t.when(() -> TypeUtils.addConstantFieldValuesReference(any())).thenAnswer(_ -> null);
            t.verify(TypeUtils::markCustomAnnotations, times(1));

            Api resultApi = scanner.scan(elements);

            assertNotNull(resultApi, "scan should return an Api instance");
            // Ensure the package got registered in the api via visitPackage
            assertNotNull(resultApi.getPackageNode("com.scan.pkg"));
        }
    }

    @Test
    void run_withElements() {

        mockDocletEnvironment();
        List<Element> elements = new ArrayList<>();
        ModuleElement moduleMock = mockModule("mockmodule");
        elements.add(moduleMock);
        PackageElement packageMock = mockPackage("mockpackage");
        elements.add(packageMock);
        mockIncludedElements(elements);

        ApiScanner apiScanner = new ApiScanner(docletEnvironmentMock);
        Api api = apiScanner.scan(docletEnvironmentMock.getIncludedElements());

        assertNotNull(api);
        assertNotNull(apiScanner.includedNames);
        assertEquals(1, apiScanner.includedNames.size());
        assertTrue(apiScanner.isIncludedElement("mockpackage"));
    }

    @Test
    void visitModule() {
        mockDocletEnvironment();
        List<Element> elements = new ArrayList<>();
        ModuleElement moduleMock = mockModule("mockmodule");
        elements.add(moduleMock);
        PackageElement packageMock = mockPackage("mockpackage");
        elements.add(packageMock);
        mockIncludedElements(elements);

        ApiScanner apiScanner = new ApiScanner(docletEnvironmentMock);
        apiScanner.scan(docletEnvironmentMock.getIncludedElements());
        apiScanner.visitModule(moduleMock, 1);

        List<ModuleNode> moduleList = apiScanner.api.getModules();
        assertEquals(1, moduleList.size());
    }

    @Test
    void visitPackage() {
        mockDocletEnvironment();
        List<Element> elements = new ArrayList<>();
        ModuleElement moduleMock = mockModule("mockmodule");
        elements.add(moduleMock);
        PackageElement packageMock = mockPackage("mockpackage");
        elements.add(packageMock);
        mockIncludedElements(elements);

        ApiScanner apiScanner = new ApiScanner(docletEnvironmentMock);
        apiScanner.scan(docletEnvironmentMock.getIncludedElements());

        PackageElement parentPackageElement = mock(PackageElement.class);
        Name packageName = mockName("mockparentpackage");
        when (parentPackageElement.getQualifiedName()).thenReturn(packageName);
        when (packageMock.getEnclosingElement()).thenReturn(parentPackageElement);
        PackageNode parentPackageNode = new PackageNode("mockparentpackage");
        apiScanner.api.addPackage(parentPackageNode);

        apiScanner.visitPackage(packageMock, 1);

        List<PackageNode> packageList = apiScanner.api.getPackages();
        assertEquals(2, packageList.size());
        PackageNode newPackage = packageList.get(1);
        assertEquals("mockpackage", newPackage.getName());
        assertEquals(1, parentPackageNode.getPackages().size());
        assertEquals("mockpackage", parentPackageNode.getPackages().getFirst().getName());
    }

    @Test
    void visitType() {
        mockDocletEnvironment();
        List<Element> elements = new ArrayList<>();
        elements.add(mockModule("mockmodule"));
        PackageElement packageMock = mockPackage("mockpackage");
        elements.add(packageMock);
        TypeElement typeMock = mockType("mocktype", packageMock);
        elements.add(typeMock);
        mockIncludedElements(elements);

        ApiScanner apiScanner = new ApiScanner(docletEnvironmentMock);
        apiScanner.scan(docletEnvironmentMock.getIncludedElements());

        PackageNode packageNode = new PackageNode("mockpackage");
        apiScanner.api.addPackage(packageNode);

        apiScanner.visitType(typeMock, 1);

        List<TypeView> moduleList = apiScanner.api.getTypes();
        assertEquals(1, moduleList.size());
    }

    @Test
    void visitVariable() {
        mockDocletEnvironment();
        List<Element> elements = new ArrayList<>();
        elements.add(mockModule("mockmodule"));
        PackageElement packageMock = mockPackage("mockpackage");
        elements.add(packageMock);
        TypeElement typeMock = mockType("mocktype", packageMock);
        elements.add(typeMock);
        VariableElement variableMock = mockVariable("mockfield", typeMock);
        elements.add(variableMock);
        mockIncludedElements(elements);

        ApiScanner apiScanner = new ApiScanner(docletEnvironmentMock);
        apiScanner.scan(docletEnvironmentMock.getIncludedElements());

        PackageNode packageNode = new PackageNode("mockpackage");
        apiScanner.api.addPackage(packageNode);
        TypeNode typeNode = new TypeNode("mocktype", "mockpackage");
        apiScanner.api.addType(typeNode);

        apiScanner.visitType(typeMock, 1);
        apiScanner.visitVariable(variableMock, 1);

        List<FieldNode> fieldList = typeNode.getFields();
        assertEquals(1, fieldList.size());
    }

    @Disabled("Methods won't appear in Type until after TextAssembler runs")
    @Test
    void visitExecutable() {
        mockDocletEnvironment();
        List<Element> elements = new ArrayList<>();
        elements.add(mockModule("mockmodule"));
        PackageElement packageMock = mockPackage("mockpackage");
        elements.add(packageMock);
        TypeElement typeMock = mockType("mocktype", packageMock);
        elements.add(typeMock);
        ExecutableElement executableMock = mockExecutable("mockmethod", typeMock);
        elements.add(executableMock);
        mockIncludedElements(elements);

        VariableElement parameterMock = mockMethodParameter("mockparameter", typeMock, executableMock);
        elements.add(parameterMock);
        mockIncludedElements(elements);

        ApiScanner apiScanner = new ApiScanner(docletEnvironmentMock);
        apiScanner.scan(docletEnvironmentMock.getIncludedElements());

        PackageNode packageNode = new PackageNode("mockpackage");
        apiScanner.api.addPackage(packageNode);
        TypeNode typeNode = new TypeNode("mocktype", "mockpackage");
        apiScanner.api.addType(typeNode);
        DocTree dct = mockDocCommentTree_TEXT("plain text");
        when(treeUtilsMock.getDocCommentTree(executableMock)).thenReturn((DocCommentTree)dct);

        apiScanner.visitType(typeMock, 1);
        apiScanner.visitExecutable(executableMock, 1);

        List<MethodNode> methodList = typeNode.getMethods();
        assertEquals(1, methodList.size());

        MethodNode methodNode = methodList.getFirst();
        List<ParamNode> paramList = methodNode.getParams();
        assertEquals(1, paramList.size());
    }

    @Disabled("Methods won't appear in Type until after TextAssembler runs")
    @Test
    void visitExecutable_constructor() {
        mockDocletEnvironment();
        List<Element> elements = new ArrayList<>();
        elements.add(mockModule("mockmodule"));
        PackageElement packageMock = mockPackage("mockpackage");
        elements.add(packageMock);
        TypeElement typeMock = mockType("mocktype", packageMock);
        elements.add(typeMock);
        ExecutableElement constructorMock = mockExecutable("mockmethod", typeMock);
        when(constructorMock.getKind()).thenReturn(ElementKind.CONSTRUCTOR);
        elements.add(constructorMock);
        mockIncludedElements(elements);

        ApiScanner apiScanner = new ApiScanner(docletEnvironmentMock);
        apiScanner.scan(docletEnvironmentMock.getIncludedElements());

        PackageNode packageNode = new PackageNode("mockpackage");
        apiScanner.api.addPackage(packageNode);
        TypeNode typeNode = new TypeNode("mocktype", "mockpackage");
        apiScanner.api.addType(typeNode);

        apiScanner.visitType(typeMock, 1);
        apiScanner.visitExecutable(constructorMock, 1);

        List<MethodNode> constructorList = typeNode.getConstructors();
        assertEquals(1, constructorList.size());
    }

    @Test
    void visitTypeParameter() {
        mockDocletEnvironment();
        List<Element> elements = new ArrayList<>();
        elements.add(mockModule("mockmodule"));
        PackageElement packageMock = mockPackage("mockpackage");
        elements.add(packageMock);
        TypeElement typeMock = mockType("mocktype", packageMock);
        elements.add(typeMock);
        TypeParameterElement parameterMock = mockTypeParameter("parametermethod", typeMock);
        elements.add(parameterMock);
        mockIncludedElements(elements);

        ApiScanner apiScanner = new ApiScanner(docletEnvironmentMock);
        apiScanner.scan(docletEnvironmentMock.getIncludedElements());

        PackageNode packageNode = new PackageNode("mockpackage");
        apiScanner.api.addPackage(packageNode);
        TypeNode typeNode = new TypeNode("mocktype", "mockpackage");
        apiScanner.api.addType(typeNode);

        apiScanner.visitType(typeMock, 1);
        apiScanner.visitTypeParameter(parameterMock, 1);

        assertNotNull(apiScanner.api);
    }

    @Test
    void isIncludedElement() {
        mockDocletEnvironment();
        List<Element> elements = new ArrayList<>();
        elements.add(mockModule("mockmodule"));
        mockIncludedElements(elements);

        ApiScanner apiScanner = new ApiScanner(docletEnvironmentMock);
        apiScanner.scan(docletEnvironmentMock.getIncludedElements());

        Element e = mockPackage("mockpackage");
        assertFalse(apiScanner.isIncludedElement(e));
    }
}


