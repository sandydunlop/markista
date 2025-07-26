package io.github.sandydunlop.markista.util;

import io.github.sandydunlop.markista.model.DirectiveNode;

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

import jdk.javadoc.doclet.DocletEnvironment;

public class ModuleDirectiveGenerator {
    private static DocletEnvironment environment;

    private ModuleDirectiveGenerator() {
        // Utility class, no instantiation
    }

    public static void setEnvironment(DocletEnvironment env) {
        environment = env;
    }

    public static DirectiveNode createFrom(Directive directive) {
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

    public static DirectiveNode createRequiresDirective(Directive directive) {
        DirectiveNode.Kind kind = DirectiveNode.Kind.REQUIRES;
        RequiresDirective requires = (RequiresDirective) directive;
        Element dependency = requires.getDependency();
        ModuleElement directiveModuleElement = environment.getElementUtils().getModuleOf(dependency);
        String name = directiveModuleElement.getQualifiedName().toString();
        boolean transitive = requires.isTransitive();
        return new DirectiveNode(kind, name, transitive);
    }

    public static DirectiveNode createExportsDirective(Directive directive) {
        DirectiveNode.Kind kind = DirectiveNode.Kind.EXPORTS;
        ExportsDirective exports = (ExportsDirective) directive;
        PackageElement directivePackageElement = exports.getPackage();
        String name = directivePackageElement.getQualifiedName().toString();
        List<? extends ModuleElement> modules = exports.getTargetModules();
        DirectiveNode directiveNode = new DirectiveNode(kind, name);
        if (modules != null) {
            for (ModuleElement moduleElement : modules) {
                directiveNode.addPackage(moduleElement.getQualifiedName().toString());
            }
        }
        return directiveNode; 
    }

    public static DirectiveNode createOpensDirective(Directive directive) {
        DirectiveNode.Kind kind = DirectiveNode.Kind.OPENS;
        OpensDirective opens = (OpensDirective) directive;
        PackageElement directivePackageElement = opens.getPackage();
        String name = directivePackageElement.getQualifiedName().toString();
        List<? extends ModuleElement> modules = opens.getTargetModules();
        DirectiveNode directiveNode = new DirectiveNode(kind, name);
        if (modules != null) {
            for (ModuleElement moduleElement : modules) {
                directiveNode.addPackage(moduleElement.getQualifiedName().toString());
            }
        }
        return directiveNode; 
    }

    public static DirectiveNode createUsesDirective(Directive directive) {
        DirectiveNode.Kind kind = DirectiveNode.Kind.USES;
        UsesDirective uses = (UsesDirective) directive;
        String name = uses.getService().getQualifiedName().toString();
        return new DirectiveNode(kind, name);
    }

    public static DirectiveNode createProvidesDirective(Directive directive) {
        DirectiveNode.Kind kind = DirectiveNode.Kind.PROVIDES;
        ProvidesDirective provides = (ProvidesDirective) directive;
        TypeElement service = provides.getService();
        String name = service.getQualifiedName().toString();
        DirectiveNode directiveNode = new DirectiveNode(kind, name);
        TypeUtils.setImplementations(directiveNode, provides.getImplementations());
        TypeUtils.setInterfaces(directiveNode, service.getInterfaces());
        return directiveNode;
    }
}