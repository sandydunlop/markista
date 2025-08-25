package io.github.sandydunlop.markista.scanning;

import io.github.sandydunlop.markista.core.Configuration;
import io.github.sandydunlop.markista.core.Context;
import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.DirectiveNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementScanner9;
import javax.tools.JavaFileObject;

import jdk.javadoc.doclet.DocletEnvironment;

/// A scanner that walks the language model elements provided by the Javadoc doclet 
/// environment and builds an Api model representing the discovered modules, packages,
/// types, and members. The scanner delegates most element-to-model conversion logic
/// to TypeUtils, and it records a set of included element names so filtering can be
/// applied when only a subset of elements should be documented.
/// 
/// This class extends ElementScanner9 so it can visit elements in source order and
/// recursively walk nested elements. The scanner keeps track of the current ModuleNode
/// being populated and updates the Api instance as elements are encountered. 
@java.lang.SuppressWarnings("squid:S110") // There is no way around this.
public class ApiScanner extends ElementScanner9<Void, Integer> {
    /// The shared Context singleton providing logging and configuration access.
    private final Context ctx;

    /// The Api model being populated by this scanner. 
    Api api;

    /// The doclet environment used to obtain Javadoc doc trees and element utilities.
    private final DocletEnvironment environment;

    /// The unnamed module node reprsenting package elements not in an explicit module. 
    private final ModuleNode unnamedModule;

    /// The module node currently being populated during a scan. private ModuleNode currentModule;
    private ModuleNode currentModule;

    /// A set of fully-qualified names (packages and types) included in the scan invocation. 
    HashSet<String> includedNames;

    /// Initializes the ApiScanner with access to the doclet environment.
    /// The doclet environment provides tools for processing API elements, types, and documentation.
    /// @param environment Represents the operating environment of a single invocation of the doclet. 
    public ApiScanner(DocletEnvironment environment) {
        this.environment = environment;
        api = new Api(Configuration.getDocTitle());
        unnamedModule = api.getUnnamedModuleNode();
        currentModule = api.getUnnamedModuleNode();
        ctx = Context.getInstance();
    }

    /// Scan the given set of top-level elements and return the built Api model.
    /// This method performs setup actions (register included elements, initialize
    /// TypeUtils with the Api and environment), executes the scan, performs some
    /// post-processing (constant value reference collection, annotation marking),
    /// computes the unnamed module source path and sorts the final Api model.
    ///
    /// @param elements top-level elements to scan (packages and types)
    /// @return the fully-populated Api model
    public Api scan(Set<? extends Element> elements) {
        processIncludedElements(elements);
        TypeUtils.init(api, environment);
        scan(elements, 0);
        TypeUtils.addConstantFieldValuesReference(currentModule);
        TypeUtils.markCustomAnnotations();
        calculateUnnamedModuleSourcePath();
        api.sort();
        return api;
    }

    /// Populate the includedNames set from the provided element set.
    /// Only package and type elements contribute names. The set is used by
    /// isIncludedElement to quickly decide if an element should be processed.
    /// @param elements the elements passed to the doclet for scanning
    private void processIncludedElements(Set<? extends Element> elements) {
        includedNames = new HashSet<>();
        for (Element element : elements) {
            if (element instanceof PackageElement packageElement) {
                includedNames.add(packageElement.getQualifiedName().toString());
            } else if (element instanceof TypeElement typeElement) {
                includedNames.add(typeElement.getQualifiedName().toString());
            }
        }
    }

    /// Check whether an element with the given qualifiedName was explicitly included
    /// in the Javadoc invocation (via the elements set passed to the doclet).
    /// @param qualifiedName the fully-qualified package or type name to test
    /// @return true if the qualifiedName is present in the includedNames set
    boolean isIncludedElement(String qualifiedName) {
        return includedNames.contains(qualifiedName);
    }

    /// Determine whether an Element should be treated as included by looking up
    /// the enclosing type name and checking the includedNames set.
    /// This overload is used for members whose direct qualified name is not directly
    /// present in includedNames but whose enclosing type may have been included.
    /// @param e the element to test (typically a member whose enclosing element is a type)
    /// @return true if the element's enclosing type was included in the elements set
    boolean isIncludedElement(Element e) {
        if (e.getEnclosingElement() instanceof TypeElement typeElement) {
            return isIncludedElement(typeElement.getQualifiedName().toString());
        }
        return false;
    }

    /// Derive an appropriate source root for the unnamed module by inspecting the
    /// source path of the first package contained in the unnamed module. If the
    /// unnamed module contains no packages this is a no-op.
    private void calculateUnnamedModuleSourcePath() {
        if (unnamedModule.getPackages().isEmpty()) return;
        PackageNode pkg = unnamedModule.getPackages().getFirst();
        String separator = java.nio.file.FileSystems.getDefault().getSeparator();
        String nameAsPath = pkg.getName().replace(".", separator);
        String root = pkg.getSourcePath().replace(nameAsPath, "");
        unnamedModule.setSourcePath(root);
    }

    @Override
    public Void scan(Element e, Integer depth) {
        return super.scan(e, depth + 1);
    }

    /// Visit a module element and create or reuse a ModuleNode for it.
    /// The module's module-info.java presence and source path are discovered,
    /// directives are added, and the module is registered with the Api model.
    @Override
    public Void visitModule(ModuleElement e, Integer depth) {
        ModuleNode mod;
        if (e.getQualifiedName().toString().isEmpty()) {
            ctx.setModuleName("");
            mod = unnamedModule;
            if (Configuration.getVerbose()) {
                ctx.reportInfo("[ MODULE] UNNAMED");
            }
        } else {
            mod = api.getModuleNode(e.getQualifiedName().toString());
        }
        if (mod == null) {
            mod = new ModuleNode(e.getQualifiedName().toString());
            ctx.setModuleName(mod.getName());
            if (Configuration.getVerbose()) {
                ctx.reportInfo(String.format("[ MODULE] %s", mod.getName()));
            }
            File moduleInfo = getModuleInfoFile(e);
            if (moduleInfo != null) {
                mod.setHasModuleInfo(true);
                mod.setSourcePath(moduleInfo.toPath().getParent().toString());
            }
            TypeUtils.setDocumentation(mod, e);
            List<? extends Directive>  directives = e.getDirectives();
            for (Directive directive : directives) {
                DirectiveNode moduleDirective = ModuleDirectives.createFrom(directive);
                mod.addDirective(moduleDirective);
            }
            api.addModule(mod);
        }
        currentModule = mod;
        return super.visitModule(e, depth);
    }

    /// Visit a package element and, if it was included, create a PackageNode and
    /// attach it to the current module and to the Api model. Package source path
    /// and documentation are configured via TypeUtils helpers.
    @Override
    public Void visitPackage(PackageElement ee, Integer depth) {
        if (isIncludedElement(ee.getQualifiedName().toString())) {
            PackageNode pkg = api.getPackageNode(ee.getQualifiedName().toString());
            if (pkg == null) {
                pkg = new PackageNode(ee.getQualifiedName().toString());
                ctx.setPackageName(pkg.getName());
                if (Configuration.getVerbose()) {
                    ctx.reportInfo(String.format("[PACKAGE] %s", pkg.getName()));
                }
                TypeUtils.setPackageSourcePath(pkg, ee);
                pkg.setModuleName(currentModule.getName());
                currentModule.addPackage(pkg);
                TypeUtils.setDocumentation(pkg, ee);
                api.addPackage(pkg);
                Element enclosing = ee.getEnclosingElement();
                if (enclosing instanceof PackageElement enclosingPackageElement) {
                    PackageNode owner = api.getPackageNode(enclosingPackageElement.getQualifiedName().toString());
                    if (owner != null) {
                        owner.addPackage(pkg);
                    }
                }
            }
        }
        return super.visitPackage(ee, depth);
    }

    /// Visit a type element (class/interface/enum/annotation) and create a TypeNode
    /// representation if the type is included and TypeUtils considers it part of the API.
    /// This also records the source file path when available.
    @Override
    public Void visitType(TypeElement e, Integer depth) { 
        if (isIncludedElement(e.getQualifiedName().toString()) && TypeUtils.isIncludedInApi(e)){
            TypeUtils.nodeFromElement(e);
        }
        return super.visitType(e, depth);
    }

    /// Visit an executable element (method or constructor), convert it to a MethodNode
    /// if included, and populate Javadoc-derived fields such as first sentence, body,
    /// return description, references and since information using TypeUtils helpers.
    @Override
    public Void visitExecutable(ExecutableElement ee, Integer depth) {
        if (isIncludedElement(ee) && TypeUtils.isIncludedInApi(ee)){
            TypeUtils.nodeFromElement(ee);
        }
        return super.visitExecutable(ee, depth);
    }

    /// Visit a variable element and, if it is a field included in the API, convert it
    /// to a FieldNode, record any constant value, and populate documentation and modifiers.
    @Override
    public Void visitVariable(VariableElement ve, Integer depth) {
        if (isIncludedElement(ve) && TypeUtils.isIncludedInApi(ve) && ve.getKind() == ElementKind.FIELD) {
            TypeUtils.nodeFromElement(ve);
        }
        return super.visitVariable(ve, depth);
    }

    /// Visit a type parameter. This method delegates to scanning of enclosed elements
    /// so that bounds and other nested elements are visited.
    @Override
    public Void visitTypeParameter(TypeParameterElement e, Integer depth) {
        return scan(e.getEnclosedElements(), depth);
    }

    /// Visit a record component.
    @Override
    public Void visitRecordComponent(RecordComponentElement e, Integer depth) {
        return null;
    }

    /// Retrieve the module-info.java file for the given ModuleElement if available.
    /// This helper inspects the JavaFileObject associated with the module and returns
    /// a File when the file name ends with "module-info.java".
    /// @param moduleElement the ModuleElement to inspect
    /// @return a File pointing to the module-info.java source or null if none found
    public File getModuleInfoFile(ModuleElement moduleElement) {
        JavaFileObject jfo = environment.getElementUtils().getFileObjectOf(moduleElement);

        if (jfo != null && jfo.getName().endsWith("module-info.java")) {
            return new File(jfo.toUri());
        }
        return null;
    }
}
