package io.github.sandydunlop.markista.scanning;

import io.github.sandydunlop.markista.MockedDocletEnvironment;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.ClassNode;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.ParamNode;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.model.TypeReference;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
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
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiScannerTests extends MockedDocletEnvironment {
    private static Context ctx;
    private PackageNode packageNode;
    private Api api;
    private ModuleNode unnamedModule = new ModuleNode("");

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
        api = new Api("Test API");
        packageNode = new PackageNode("io.github.sandydunlop.markista.model");
        api.addPackage(packageNode);
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

        Api api2 = scanner2.api;
        assertEquals(1, api2.getModules().size());
        ModuleNode moduleNode = api2.getModules().get(0);
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

        ApiScanner scanner2 = new ApiScanner(mockEnvironment);
        scanner2.visitModule(moduleElement, Integer.valueOf(0));

        Api api2 = scanner2.api;
        assertEquals(0, api2.getModules().size());
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


    @Test
    void calculateUnnamedModuleSourcePath_computes_root_and_sets_module_source_path() throws Exception {
        ApiScanner as = new ApiScanner(docletEnvironmentMock);
        api = as.api;

        ModuleNode unnamed = api.getUnnamedModuleNode();

        // Create a mocked PackageNode and attach to unnamed module packages deque
        PackageNode pkg = mock(PackageNode.class);
        when(pkg.getName()).thenReturn("com.example");
        Path srcPath = Path.of("root", "src", "com", "example");
        when(pkg.getSourcePath()).thenReturn(srcPath.toString());

        // Add to unnamed module packages (LinkedList or similar)
        unnamed.getPackages().add(pkg);

        as.calculateUnnamedModuleSourcePath();

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
        Api api2 = apiScanner.scan(docletEnvironmentMock.getIncludedElements());

        assertNotNull(api2);
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

        PackageNode packageNode2 = new PackageNode("mockpackage");
        apiScanner.api.addPackage(packageNode2);

        apiScanner.visitType(typeMock, 1);

        List<TypeNode> moduleList = apiScanner.api.getTypes();
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

        PackageNode packageNode2 = new PackageNode("mockpackage");
        apiScanner.api.addPackage(packageNode2);
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

        PackageNode packageNode2 = new PackageNode("mockpackage");
        apiScanner.api.addPackage(packageNode2);
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

        PackageNode packageNode2 = new PackageNode("mockpackage");
        apiScanner.api.addPackage(packageNode2);
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

        PackageNode packageNode2 = new PackageNode("mockpackage");
        apiScanner.api.addPackage(packageNode2);
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


    @Test
    void addConstantFieldValuesReference() {
        ClassNode classNode2 = new ClassNode("Node", packageNode.getName());
        TypeReference supertype = TypeReference.to("io.github.sandydunlop.markista.model.Node");
        classNode2.getSupertypes().add(supertype);
        api.addType(classNode2);
        FieldNode fieldNode = new FieldNode("int", "field");
        fieldNode.setConstantValue("1");
        classNode2.addField(fieldNode);
        ApiScanner apiScanner = new ApiScanner(docletEnvironmentMock);
        apiScanner.api = api;
        apiScanner.addConstantFieldValuesReference(unnamedModule);
        assertEquals(1, unnamedModule.getConstantValues().size());
    }
}


