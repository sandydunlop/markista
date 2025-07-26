package io.github.sandydunlop.markista.doclet;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.ReturnTree;

import io.github.sandydunlop.markista.model.Api;
import io.github.sandydunlop.markista.model.FieldNode;
import io.github.sandydunlop.markista.model.MethodNode;
import io.github.sandydunlop.markista.model.DirectiveNode;
import io.github.sandydunlop.markista.model.ModuleNode;
import io.github.sandydunlop.markista.model.PackageNode;
import io.github.sandydunlop.markista.model.PackageOwner;
import io.github.sandydunlop.markista.model.TypeNode;
import io.github.sandydunlop.markista.util.Configuration;
import io.github.sandydunlop.markista.util.ModuleDirectiveGenerator;
import io.github.sandydunlop.markista.util.TypeUtils;

import java.io.Serializable;
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
import javax.tools.Diagnostic;

import jdk.javadoc.doclet.DocletEnvironment;

/// A class that scans code and generates an API tree representing code and Javadoc comments.
public class ApiScanner extends ElementScanner9<Void, Integer> {
    private Api api;
    private DocletEnvironment environment;
    private ModuleNode unnamedModule;
    private ModuleNode currentModule;

    public ApiScanner(DocletEnvironment environment) {
        this.environment = environment;
        api = new Api();
        unnamedModule = api.getUnnamedModuleNode();
        currentModule = api.getUnnamedModuleNode();
    }

    public Api scan(Set<? extends Element> elements) {
        TypeUtils.init(api, environment);
        scan(elements, 0);
        TypeUtils.addConstantFieldValuesReference(currentModule);
        return api;
    }

    @Override
    public Void scan(Element e, Integer depth) {
        return super.scan(e, depth + 1);
    }

    @Override
    public Void visitModule(ModuleElement e, Integer depth) {
        ModuleNode mod;
        if (e.getQualifiedName().toString().isEmpty()) {
            mod = unnamedModule;
            if (Configuration.getVerbose()) {
                Configuration.getReporter().print(Diagnostic.Kind.NOTE, "UNNAMED MODULE");
            }
        } else {
            mod = api.getModuleNode(e.getQualifiedName().toString());
        }
        if (mod == null) {
            mod = new ModuleNode(e.getQualifiedName().toString());
            if (Configuration.getVerbose()) {
                Configuration.getReporter().print(Diagnostic.Kind.NOTE, "MODULE: " + mod.getName());
            }
            TypeUtils.setDocumentation(mod, e);
            List<? extends Directive>  directives = e.getDirectives();
            for (Directive directive : directives) {
                DirectiveNode moduleDirective = ModuleDirectiveGenerator.createFrom(directive);
                mod.addDirective(moduleDirective);
            }
            api.addModule(mod);
        }
        currentModule = mod;
        return super.visitModule(e, depth);
    }

    @Override
    public Void visitPackage(PackageElement ee, Integer depth) {
        PackageNode pkg = api.getPackageNode(ee.getQualifiedName().toString());
        if (pkg == null) {
            pkg = new PackageNode(ee.getQualifiedName().toString());
            if (Configuration.getVerbose()) {
                Configuration.getReporter().print(Diagnostic.Kind.NOTE, "  PACKAGE: " + pkg.getName());
            }
            if (environment.getElementUtils().getModuleOf(ee) != null && currentModule != null) {
                pkg.setModule(currentModule);
                currentModule.addPackage(pkg);
            }
            TypeUtils.setDocumentation(pkg, ee);
            api.addPackage(pkg);
            Element enclosing = ee.getEnclosingElement();
            if (enclosing.getKind() == ElementKind.PACKAGE) {
                PackageOwner owner = api.getPackageNode(ee.getQualifiedName().toString());
                if (owner != null) {
                    owner.getPackages().add(pkg);
                }
            }
        }
        return super.visitPackage(ee, depth);
    }

    @Override
    public Void visitType(TypeElement e, Integer depth) { 
        if (TypeUtils.isIncludedInApi(e)){
            TypeNode typeNode = TypeUtils.nodeFromElement(e);
            TypeUtils.setDocumentation(typeNode, e);
        }
        return super.visitType(e, depth);
    }

    @Override
    public Void visitExecutable(ExecutableElement ee, Integer depth) {
        if (TypeUtils.isIncludedInApi(ee)){
            MethodNode methodNode = TypeUtils.nodeFromElement(ee);
            if (methodNode != null) {
                DocCommentTree dct = environment.getDocTrees().getDocCommentTree(ee);
                TypeUtils.setDeprecationStatus(methodNode, ee, dct);
                if (dct != null) {
                    methodNode.setFirstSentence(TypeUtils.createText(dct.getFirstSentence()));
                    methodNode.setBody(TypeUtils.createText(dct.getBody()));
                    methodNode.setFullBody(TypeUtils.createText(dct.getFullBody()));
                    ReturnTree returnTree = TypeUtils.getReturnTree(dct);
                    if (returnTree != null) {
                        methodNode.setReturnDescription(TypeUtils.createText(returnTree.getDescription()));
                    }
                    methodNode.setReferences(TypeUtils.getReferences(dct));
                    methodNode.setSince(TypeUtils.getSince(dct));
                }
            }
        }
        return super.visitExecutable(ee, depth);
    }

    @Override
    public Void visitVariable(VariableElement ve, Integer depth) {
        if (TypeUtils.isIncludedInApi(ve) && ve.getKind() == ElementKind.FIELD) {
            FieldNode fieldNode = TypeUtils.nodeFromElement(ve);
            if (fieldNode != null ) {
                fieldNode.setConstantValue((Serializable) ve.getConstantValue());
                DocCommentTree dct = environment.getDocTrees().getDocCommentTree(ve);
                TypeUtils.setDocumentation(fieldNode, ve);
                TypeUtils.setModifiers(fieldNode, ve.getModifiers());
                TypeUtils.setDeprecationStatus(fieldNode, ve, dct);
            }
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

}
