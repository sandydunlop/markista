package io.github.sandydunlop.markista.util;

import io.github.sandydunlop.markista.model.ModuleDirectiveNode;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.ModuleElement.DirectiveKind;
import javax.lang.model.element.ModuleElement.ExportsDirective;
import javax.lang.model.element.ModuleElement.OpensDirective;
import javax.lang.model.element.ModuleElement.ProvidesDirective;
import javax.lang.model.element.ModuleElement.RequiresDirective;
import javax.lang.model.element.ModuleElement.UsesDirective;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import jdk.javadoc.doclet.DocletEnvironment;

public class ModuleDirectiveGenerator {
    private static DocletEnvironment environment;

    private ModuleDirectiveGenerator() {
        // Utility class, no instantiation
    }

    public static void setEnvironment(DocletEnvironment env) {
        environment = env;
    }

    public static ModuleDirectiveNode createFrom(Directive directive) {
        switch(directive.getKind()) {
            case DirectiveKind.REQUIRES:
                return createRequiresDirective(directive);
            case DirectiveKind.EXPORTS:
                return createExportsDirective(directive);
            case DirectiveKind.OPENS:
                return createOpensDirective(directive);
            case DirectiveKind.USES:
                return createUsesDirective(directive);
            case DirectiveKind.PROVIDES:
                return createProvidesDirective(directive);
            default:
                return null;
        }
    }

    public static ModuleDirectiveNode createRequiresDirective(Directive directive) {
        ModuleDirectiveNode.Kind kind = ModuleDirectiveNode.Kind.REQUIRES;
        RequiresDirective requires = (RequiresDirective) directive;
        Element dependency = requires.getDependency();
        ModuleElement directiveModuleElement = environment.getElementUtils().getModuleOf(dependency);
        String name = directiveModuleElement.getQualifiedName().toString();
        boolean transitive = requires.isTransitive();
        return new ModuleDirectiveNode(kind, name, transitive);
    }

    public static ModuleDirectiveNode createExportsDirective(Directive directive) {
        ModuleDirectiveNode.Kind kind = ModuleDirectiveNode.Kind.EXPORTS;
        ExportsDirective exports = (ExportsDirective) directive;
        PackageElement directivePackageElement = exports.getPackage();
        String name = directivePackageElement.getQualifiedName().toString();
        List<? extends ModuleElement> modules = exports.getTargetModules();
        ModuleDirectiveNode directiveNode = new ModuleDirectiveNode(kind, name);
        if (modules != null) {
            for (ModuleElement moduleElement : modules) {
                directiveNode.addPackage(moduleElement.getQualifiedName().toString());
            }
        }
        return directiveNode; 
    }

    public static ModuleDirectiveNode createOpensDirective(Directive directive) {
        ModuleDirectiveNode.Kind kind = ModuleDirectiveNode.Kind.OPENS;
        OpensDirective opens = (OpensDirective) directive;
        PackageElement directivePackageElement = opens.getPackage();
        String name = directivePackageElement.getQualifiedName().toString();
        List<? extends ModuleElement> modules = opens.getTargetModules();
        ModuleDirectiveNode directiveNode = new ModuleDirectiveNode(kind, name);
        if (modules != null) {
            for (ModuleElement moduleElement : modules) {
                directiveNode.addPackage(moduleElement.getQualifiedName().toString());
            }
        }
        return directiveNode; 
    }

    public static ModuleDirectiveNode createUsesDirective(Directive directive) {
        ModuleDirectiveNode.Kind kind = ModuleDirectiveNode.Kind.USES;
        UsesDirective uses = (UsesDirective) directive;
        //TODO details
        String name = "";
        return new ModuleDirectiveNode(kind, name);
    }

    public static ModuleDirectiveNode createProvidesDirective(Directive directive) {
        ModuleDirectiveNode.Kind kind = ModuleDirectiveNode.Kind.PROVIDES;
        ProvidesDirective provides = (ProvidesDirective) directive;
        TypeElement service = provides.getService();
        //TODO details 'provides interface/abstract with implementation'
        String name = service.getQualifiedName().toString(); // Implementation
        ModuleDirectiveNode directiveNode = new ModuleDirectiveNode(kind, name);
        List<? extends TypeMirror> interfaces = service.getInterfaces();
        return directiveNode;
    }
}