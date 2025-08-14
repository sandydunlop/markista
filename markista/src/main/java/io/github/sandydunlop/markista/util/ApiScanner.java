package io.github.sandydunlop.markista.util;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.DirectiveNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.PackageOwnerInterface;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementScanner9;
import javax.tools.JavaFileObject;

import jdk.javadoc.doclet.DocletEnvironment;

/// A class that scans code and generates an API tree representing code and Javadoc comments.
public class ApiScanner extends ElementScanner9<Void, Integer> {
    private final Context ctx;
    Api api;
    private final DocletEnvironment environment;
    private final ModuleNode unnamedModule;
    private ModuleNode currentModule;
    private HashSet<String> includedNames;

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

    /// The starting point for a scan of the API structure. This method sets up
    /// the environment and begins the scan of the API.
    /// @param elements a list of language model elements
    /// @return An [Api] object representing the entire model of the API that's being documented
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

    private boolean isIncludedElement(String qualifiedName) {
        return includedNames.contains(qualifiedName);
    }

    private boolean isIncludedElement(Element e) {
        if (e.getEnclosingElement() instanceof TypeElement typeElement) {
            return isIncludedElement(typeElement.getQualifiedName().toString());
        }
        return false;
    }

    private void calculateUnnamedModuleSourcePath() {
        if (unnamedModule.getPackages().isEmpty()) return;
        PackageNode pkg = unnamedModule.getPackages().getFirst();
        String separator = java.nio.file.FileSystems.getDefault().getSeparator();
        String nameAsPath = pkg.getName().replace(".", separator);
        String root = pkg.getSourcePath().toString().replace(nameAsPath, "");
        unnamedModule.setSourcePath(Path.of(root));
    }

    @Override
    public Void scan(Element e, Integer depth) {
        return super.scan(e, depth + 1);
    }

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
                mod.setSourcePath(moduleInfo.toPath().getParent());
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
                pkg.setModule(currentModule);
                currentModule.addPackage(pkg);
                TypeUtils.setDocumentation(pkg, ee);
                api.addPackage(pkg);
                Element enclosing = ee.getEnclosingElement();
                if (enclosing != null && enclosing.getKind() == ElementKind.PACKAGE) {
                    PackageOwnerInterface owner = api.getPackageNode(ee.getQualifiedName().toString());
                    if (owner != null) {
                        owner.getPackages().add(pkg);
                    }
                }
            }
        }
        return super.visitPackage(ee, depth);
    }

    @Override
    public Void visitType(TypeElement e, Integer depth) { 
        if (isIncludedElement(e.getQualifiedName().toString()) && TypeUtils.isIncludedInApi(e)){
            TypeUtils.nodeFromElement(e);
        }
        return super.visitType(e, depth);
    }

    @Override
    public Void visitExecutable(ExecutableElement ee, Integer depth) {
        if (isIncludedElement(ee) && TypeUtils.isIncludedInApi(ee)){
            TypeUtils.nodeFromElement(ee);
        }
        return super.visitExecutable(ee, depth);
    }

    @Override
    public Void visitVariable(VariableElement ve, Integer depth) {
        if (isIncludedElement(ve) && TypeUtils.isIncludedInApi(ve) && ve.getKind() == ElementKind.FIELD) {
            TypeUtils.nodeFromElement(ve);
        }
        return super.visitVariable(ve, depth);
    }

    @Override
    public Void visitTypeParameter(TypeParameterElement e, Integer depth) {
        return scan(e.getEnclosedElements(), depth);
    }

    @Override
    public Void visitRecordComponent(RecordComponentElement e, Integer depth) {
        return visitUnknown(e, depth);
    }

    public File getModuleInfoFile(ModuleElement moduleElement) {
        JavaFileObject jfo = environment.getElementUtils().getFileObjectOf(moduleElement);

        if (jfo != null && jfo.getName().endsWith("module-info.java")) {
            return new File(jfo.toUri());
        }
        return null;
    }
}
