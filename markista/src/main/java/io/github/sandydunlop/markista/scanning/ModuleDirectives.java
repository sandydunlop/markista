package io.github.sandydunlop.markista.scanning;

import io.github.sandydunlop.markista.model.DirectiveNode;
import io.github.sandydunlop.markista.model.Link;

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

/// A utility class for creating [DirectiveNode] objects which encapsulate
/// the information provided by [Directive](javax.lang.model.element.ModuleElement.Directive)
/// objects scanned by the [ApiScanner][io.github.sandydunlop.markista.scanning.ApiScanner].
public class ModuleDirectives {
    private static DocletEnvironment environment;

    private ModuleDirectives() {
        // Utility class, no instantiation
    }

    /// Sets the doclet environment where it can get access to
    /// an [Elements][javax.lang.model.util.Elements] implementation
    /// to retrieve information about scanned elements.
    /// @param env The doclet environment
    public static void setEnvironment(DocletEnvironment env) {
        environment = env;
    }

    /// Creates a [DirectiveNode] to encapsulate the information provided
    /// by a module directive element.
    /// @param directive a scanned module directive.
    /// @return A DirectiveNode representing the scanned directive element.
    public static DirectiveNode createFrom(Directive directive) {
        return switch (directive.getKind()) {
            case DirectiveKind.REQUIRES -> createRequiresDirective(directive);
            case DirectiveKind.EXPORTS -> createExportsDirective(directive);
            case DirectiveKind.OPENS -> createOpensDirective(directive);
            case DirectiveKind.USES -> createUsesDirective(directive);
            case DirectiveKind.PROVIDES -> createProvidesDirective(directive);
        };
    }

    /// Creates a DirectiveNode representing a [requires](javax.lang.model.element.ModuleElement.RequiresDirective) directive.
    /// @param directive a scanned [RequiresDirective](javax.lang.model.element.ModuleElement.RequiresDirective) element.
    /// @return A DirectiveNode representing the scanned directive element.
    public static DirectiveNode createRequiresDirective(Directive directive) {
        DirectiveNode.Kind kind = DirectiveNode.Kind.REQUIRES;
        RequiresDirective requires = (RequiresDirective) directive;
        Element dependency = requires.getDependency();
        ModuleElement directiveModuleElement = environment.getElementUtils().getModuleOf(dependency);
        String name = directiveModuleElement.getQualifiedName().toString();
        boolean transitive = requires.isTransitive();
        Link reference = Link.to(name)
                .withKind(Link.Kind.MODULE)
                .withLabel(name);
        return new DirectiveNode(kind, reference, transitive);
    }

    /// Creates a DirectiveNode representing an [exports](javax.lang.model.element.ExportsDirective) directive.
    /// @param directive a scanned [ExportsDirective](javax.lang.model.element.ExportsDirective) element.
    /// @return A DirectiveNode representing the scanned directive element.
    public static DirectiveNode createExportsDirective(Directive directive) {
        DirectiveNode.Kind kind = DirectiveNode.Kind.EXPORTS;
        ExportsDirective exports = (ExportsDirective) directive;
        PackageElement directivePackageElement = exports.getPackage();
        String name = directivePackageElement.getQualifiedName().toString();
        List<? extends ModuleElement> modules = exports.getTargetModules();
        Link ref = Link.to(name)
                .withKind(Link.Kind.PACKAGE)
                .withLabel(name);
        DirectiveNode directiveNode = new DirectiveNode(kind, ref);
        if (modules != null) {
            for (ModuleElement moduleElement : modules) {
                String packageName = moduleElement.getQualifiedName().toString();
                Link reference = Link.to(packageName)
                        .withKind(Link.Kind.PACKAGE)
                        .withLabel(packageName);
                directiveNode.addPackage(reference);
            }
        }
        return directiveNode;
    }

    /// Creates a DirectiveNode representing an [opens](javax.lang.model.element.OpensDirective) directive.
    /// @param directive a scanned [OpensDirective](javax.lang.model.element.OpensDirective) element.
    /// @return A DirectiveNode representing the scanned directive element.
    public static DirectiveNode createOpensDirective(Directive directive) {
        DirectiveNode.Kind kind = DirectiveNode.Kind.OPENS;
        OpensDirective opens = (OpensDirective) directive;
        PackageElement directivePackageElement = opens.getPackage();
        String name = directivePackageElement.getQualifiedName().toString();
        List<? extends ModuleElement> modules = opens.getTargetModules();
        Link ref = Link.to(name)
                .withKind(Link.Kind.PACKAGE)
                .withLabel(name);
        DirectiveNode directiveNode = new DirectiveNode(kind, ref);
        if (modules != null) {
            for (ModuleElement moduleElement : modules) {
                String moduleName = moduleElement.getQualifiedName().toString();
                Link reference = Link.to(moduleName)
                        .withKind(Link.Kind.PACKAGE)
                        .withLabel(moduleName);
                directiveNode.addPackage(reference);
            }
        }
        return directiveNode;
    }

    /// Creates a DirectiveNode representing a [uses](javax.lang.model.element.UsesDirective) directive.
    /// @param directive a scanned [UsesDirective](javax.lang.model.element.UsesDirective) element.
    /// @return A DirectiveNode representing the scanned directive element.
    public static DirectiveNode createUsesDirective(Directive directive) {
        DirectiveNode.Kind kind = DirectiveNode.Kind.USES;
        UsesDirective uses = (UsesDirective) directive;
        String name = uses.getService().getQualifiedName().toString();
        Link ref = Link.to(name)
                .withKind(Link.Kind.TYPE)
                .withLabel(name);
        return new DirectiveNode(kind, ref);
    }

    /// Creates a DirectiveNode representing a [provides](javax.lang.model.element.ProvidesDirective) directive.
    /// @param directive a scanned [ProvidesDirective](javax.lang.model.element.ProvidesDirective) element.
    /// @return A DirectiveNode representing the scanned directive element.
    public static DirectiveNode createProvidesDirective(Directive directive) {
        DirectiveNode.Kind kind = DirectiveNode.Kind.PROVIDES;
        ProvidesDirective provides = (ProvidesDirective) directive;
        TypeElement service = provides.getService();
        String name = service.getQualifiedName().toString();
        Link ref = Link.to(name)
                .withKind(Link.Kind.TYPE)
                .withLabel(name);
        DirectiveNode directiveNode = new DirectiveNode(kind, ref);
        setImplementations(directiveNode, provides.getImplementations());
        String interfaceName = service.getQualifiedName().toString();
        Link reference = Link.to(interfaceName)
                .withKind(Link.Kind.PACKAGE)
                .withLabel(interfaceName);
        directiveNode.setInterface(reference);
        return directiveNode;
    }

    /// Adds implementation type names to a DirectiveNode.
    /// @param directiveNode The DirectiveNode to update.
    /// @param implementations List of TypeElements representing implementations.
    public static void setImplementations(DirectiveNode directiveNode, List<? extends TypeElement> implementations) {
        for (TypeElement e : implementations) {
            String implName = e.getQualifiedName().toString();
            Link reference = Link.to(implName)
                    .withKind(Link.Kind.TYPE)
                    .withLabel(implName);
            directiveNode.addImplementation(reference);
        }
    }
}